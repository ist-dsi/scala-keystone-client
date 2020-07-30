package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import org.http4s.Status.Conflict
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Project, Session}

final class Projects[F[_]: Sync: Client](baseUri: Uri, session: Session, authToken: Header)
  extends CrudService[F, Project, Project.Create, Project.Update](baseUri, "project", authToken)
  with UniqueWithinDomain[F, Project]
  with RoleAssignment[F]
  with EnableDisableEndpoints[F, Project] {

  /**
    * @param name filters the response by a project name.
    * @param domainId filters the response by a domain ID.
    * @param enabled filters the response by either enabled (true) or disabled (false) projects.
    * @param isDomain if this is specified as true, then only projects acting as a domain are included. Otherwise, only projects that are not acting as a domain are included.
    * @param parentId filters the response by a parent ID
    * @return a stream of project filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None, enabled: Option[Boolean],
           isDomain: Option[Boolean] = None, parentId: Option[String] = None): Stream[F, Project] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
      "enabled" -> enabled.map(_.toString),
      "is_domain" -> isDomain.map(_.toString),
      "parent_id" -> parentId,
    )))

  override def create(create: Project.Create, extraHeaders: Header*): F[Project] = createHandleConflict(create, extraHeaders:_*) {
    def updateIt(existingProject: Project): F[Project] = {
      if (existingProject.description != create.description || existingProject.enabled != create.enabled || existingProject.tags != create.tags) {
        val updated = Project.Update(description = create.description, enabled = Some(create.enabled), tags = Some(create.tags))
        update(existingProject.id, updated, extraHeaders:_*)
      } else {
        Sync[F].pure(existingProject)
      }
    }

    if (create.isDomain) {
      // We got a Conflict while creating a project with isDomain = true, so a project named create.name with id_domain = true must exist.
      // Currently Keystone does not accept the limit query param but it might in the future.
      // We only need 2 results to disambiguate whether the project name is unique or not.
      list(Query.fromPairs("name" -> create.name, "is_domain" -> "true", "limit" -> "2")).compile.toList.flatMap { projects =>
        if (projects.lengthIs == 1) {
          updateIt(projects.head)
        } else {
          // There is more than one Project with name `create.name` and isDomain = true. We do not have enough information to disambiguate between them.
          Sync[F].raiseError(UnexpectedStatus(Conflict))
        }
      }
    } else {
      val computeDomainId: F[String] = create.domainId match {
        case Some(domainId) => Sync[F].pure(domainId)
        // For regular projects if domain_id is not specified, but parent_id is specified, then the domain ID of the parent will be used.
        // If neither domain_id or parent_id is specified, the Identity service implementation will default to the domain to which the client’s token is scoped.
        case None => create.parentId match {
          // If the parent project doesn't exist the initial POST (performed by the createHandleConflict) will already have thrown an error
          case Some(parentId) => apply(parentId).map(project => project.domainId)
          case None => Sync[F].pure(domainIdFromScope(session.scope))
        }
      }
      computeDomainId.flatMap { domainId =>
        // We got a Conflict so we must be able to find the existing Project
        get(create.name, domainId).flatMap(updateIt)
      }
    }
  }

  override protected def updateEnable(id: String, enabled: Boolean): F[Project] = update(id, Project.Update(enabled = Some(enabled)))
}