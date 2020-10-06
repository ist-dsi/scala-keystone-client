package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{KeystoneError, Project, Scope, Session}

final class Projects[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Project, Project.Create, Project.Update](baseUri, "project", session.authToken)
  with UniqueWithinDomain[F, Project]
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
           isDomain: Option[Boolean] = None, parentId: Option[String] = None): F[List[Project]] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
      "enabled" -> enabled.map(_.toString),
      "is_domain" -> isDomain.map(_.toString),
      "parent_id" -> parentId,
    )))
  
  override def update(id: String, update: Project.Update, extraHeaders: Header*): F[Project] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override def defaultResolveConflict(existing: Project, create: Project.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[Project] = {
    val newTags =
      if (keepExistingElements) create.tags ++ existing.tags.diff(create.tags)
      else create.tags
    
    val updated = Project.Update(
      description = Option(create.description).filter(_ != existing.description),
      enabled = Option(create.enabled).filter(_ != existing.enabled),
      tags = Option(newTags).filter(_ != existing.tags),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def createOrUpdate(create: Project.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (Project, Project.Create) => F[Project] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Project] = {
    //val conflicting = """.*?it is not permitted to have two projects with the same name in the same domain : ([^.]+)\.""".r
    createHandleConflict(create, uri, extraHeaders) {
      if (create.isDomain) {
        // We got a Conflict while creating a project with isDomain = true, so a project named create.name with id_domain = true must exist.
        // Currently Keystone does not accept the limit query param but it might in the future.
        // We only need 2 results to disambiguate whether the project name is unique or not.
        list(Query.fromPairs("name" -> create.name, "is_domain" -> "true", "limit" -> "2")).flatMap {
          case List(existing) => resolveConflict(existing, create)
          case _ =>
            // TODO: I think the initial post will have already thrown a similar response
            val message = s"Cannot create a Project idempotently because more than one exists with name: ${create.name} and isDomain = true."
            Sync[F].raiseError(KeystoneError(message, Conflict.code, Conflict.reason))
        }
      } else {
        val computeDomainId: F[String] = create.domainId match {
          case Some(domainId) => Sync[F].pure(domainId)
          // For regular projects if domain_id is not specified, but parent_id is specified, then the domain ID of the parent will be used.
          // If neither domain_id or parent_id is specified, the Identity service implementation will default to the domain to which the client’s token is scoped.
          case None => create.parentId match {
            // If the parent project doesn't exist the initial POST (performed by the postHandleConflict) will already have thrown an error
            case Some(parentId) => apply(parentId).map(project => project.domainId)
            case None => Sync[F].pure(session.scopedDomainId())
          }
        }
        computeDomainId.flatMap { domainId =>
          // We got a Conflict so we must be able to find the existing Project
          apply(create.name, domainId).flatMap(resolveConflict(_, create))
        }
      }
    }
  }
  
  /** Allows performing role assignment operations on the project with `id` */
  def on(id: String): RoleAssignment[F] =
    new RoleAssignment(baseUri, Scope.Project(id), session)
  /** Allows performing role assignment operations on `project`. */
  def on(project: Project): RoleAssignment[F] = on(project.id)

  override protected def updateEnable(id: String, enabled: Boolean): F[Project] = update(id, Project.Update(enabled = Some(enabled)))
}