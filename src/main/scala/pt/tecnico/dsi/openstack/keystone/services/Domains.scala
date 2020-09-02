package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Method.DELETE
import org.http4s.Status.{Forbidden, NotFound, Successful}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Domain, Scope}

final class Domains[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Domain, Domain.Create, Domain.Update](baseUri, "domain", authToken)
  with EnableDisableEndpoints[F, Domain] {
  import dsl._

  /**
    * @param name filters the response by a domain name.
    * @param enabled filters the response by either enabled (true) or disabled (false) domains.
    * @return a stream of domains filtered by the various parameters.
    */
  def list(name: Option[String] = None, enabled: Option[Boolean]): Stream[F, Domain] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "enabled" -> enabled.map(_.toString),
    )))

  /**
    * Get detailed information about the domain specified by name, assuming it exists.
    *
    * @param name the domain name
    * @return the domain matching the name. If none exists F will contain an error.
    */
  def applyByName(name: String): F[Domain] = {
    // A domain name is globally unique across all domains.
    list(Query.fromPairs("name" -> name)).compile.lastOrError
  }
  /**
   * Get detailed information about the domain specified by name.
   *
   * @param name the domain name
   * @return a Some of the domain matching the name if it exists. A None otherwise.
   */
  def getByName(name: String): F[Option[Domain]] =
    list(Query.fromPairs("name" -> name)).compile.last

  override def create(create: Domain.Create, extraHeaders: Header*): F[Domain] = createHandleConflict(create, extraHeaders:_*) {
    // We got a conflict so a domain with this name must already exist
    applyByName(create.name).flatMap { existingDomain =>
      val updated = Domain.Update(description = create.description, enabled = Some(create.enabled))
      update(existingDomain.id, updated)
    }
  }

  /**
    * Deletes the domain. This also deletes all entities owned by the domain, such as users, groups, and projects, and any credentials
    * and granted roles that relate to those entities.
    *
    * @param id the domain id.
    * @param force if set to true, the domain will first be disabled and then deleted.
    */
  def delete(id: String, force: Boolean = false): F[Unit] = {
    DELETE(uri / id, authToken).flatMap(client.run(_).use {
      case Successful(_) | NotFound(_) => F.pure(())
      case response =>
        // If you try to delete an enabled domain you'll get a Forbidden.
        if (response.status == Forbidden && force) {
          // If force is set we try again. If that fails then the request is probably really forbidden.
          disable(id) >> super.delete(id)
        } else {
          F.raiseError(UnexpectedStatus(response.status))
        }
    })
  }
  
  /** Allows performing role assignment operations on the domain with `id` */
  def on(id: String): RoleAssignment[F] =
    new RoleAssignment(baseUri, Scope.Domain.id(id), authToken)
  /** Allows performing role assignment operations on `domain`. */
  def on(domain: Domain): RoleAssignment[F] = on(domain.id)
  
  override protected def updateEnable(id: String, enabled: Boolean): F[Domain] = update(id, Domain.Update(enabled = Some(enabled)))
}
