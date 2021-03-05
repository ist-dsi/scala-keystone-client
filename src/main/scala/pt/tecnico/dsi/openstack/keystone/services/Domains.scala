package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Concurrent
import cats.syntax.flatMap._
import org.http4s.Method.DELETE
import org.http4s.Status.{Conflict, Forbidden, Gone, NotFound, Successful}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Domain, KeystoneError, Scope, Session}

/**
 * The service class for domains.
 * @define domainModel domain
 */
final class Domains[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Domain, Domain.Create, Domain.Update](baseUri, "domain", session.authToken)
    with EnableDisableEndpoints[F, Domain] {
  
  /**
    * @param name filters the response by a domain name.
    * @param enabled filters the response by either enabled (true) or disabled (false) domains.
    * @return a stream of domains filtered by the various parameters.
    */
  def list(name: Option[String] = None, enabled: Option[Boolean]): F[List[Domain]] =
    list(Query("name" -> name, "enabled" -> enabled.map(_.toString)))
  
  /**
   * Get detailed information about the domain specified by name.
   *
   * @param name the domain name
   * @return a Some of the domain matching the name if it exists. A None otherwise.
   */
  def getByName(name: String): F[Option[Domain]] = stream("name" -> name).compile.last
  
  /**
   * Get detailed information about the domain specified by name, assuming it exists.
   *
   * @param name the domain name
   * @return the domain matching the name. If none exists F will contain an error.
   */
  def applyByName(name: String): F[Domain] =
    getByName(name).flatMap {
      case Some(domain) => F.pure(domain)
      case None => F.raiseError(new NoSuchElementException(s"""Could not find domain named "$name""""))
    }
  
  override def defaultResolveConflict(existing: Domain, create: Domain.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[Domain] = {
    val updated = Domain.Update(
      description = Option(create.description).filter(_ != existing.description),
      enabled = Option(create.enabled).filter(_ != existing.enabled),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Concurrent[F].pure(existing)
  }
  override def createOrUpdate(create: Domain.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (Domain, Domain.Create) => F[Domain] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Domain] = {
    // How do you think Openstack implements Domains? As a project with isDomain = true? Thumbs up for good implementations </sarcasm>
    val conflicting = """.*?it is not permitted to have two projects acting as domains with the same name: ([^.]+)\.""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders) {
      case KeystoneError(conflicting(name), Conflict.code, _) =>
        applyByName(name).flatMap { existing =>
          getLogger.info(s"createOrUpdate: found unique ${this.name} (id: ${existing.id}) with the correct name.")
          resolveConflict(existing, create)
        }
    }
  }
  
  override def update(id: String, update: Domain.Update, extraHeaders: Header.ToRaw*): F[Domain] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override protected def updateEnable(id: String, enabled: Boolean): F[Domain] =
    update(id, Domain.Update(enabled = Some(enabled)))
  
  /**
    * Deletes the domain. This also deletes all entities owned by the domain, such as users, groups, and projects, and any credentials
    * and granted roles that relate to those entities.
    *
    * @param id the domain id.
    * @param force if set to true, the domain will first be disabled and then deleted.
    */
  def delete(id: String, force: Boolean = false): F[Unit] = {
    import dsl._
    val request = DELETE(uri / id, authToken)
    client.run(request).use {
      case Successful(_) | NotFound(_) | Gone(_) => F.pure(())
      case Forbidden(_) if force =>
        // If you try to delete an enabled domain you'll get a Forbidden.
        // If force is set we try again. If that fails then the request is probably really forbidden.
        disable(id) >> super.delete(id)
      case response => super.defaultOnError(request, response)
    }
  }
  
  /** Allows performing role assignment operations on the domain with `id` */
  def on(id: String): RoleAssignment[F] = new RoleAssignment(baseUri, Scope.Domain.id(id), session)
  /** Allows performing role assignment operations on `domain`. */
  def on(domain: Domain): RoleAssignment[F] = on(domain.id)
}
