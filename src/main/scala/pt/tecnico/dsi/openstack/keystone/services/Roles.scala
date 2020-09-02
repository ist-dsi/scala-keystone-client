package pt.tecnico.dsi.openstack.keystone.services

import scala.annotation.nowarn
import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Status.Conflict
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Assignment, Domain, Group, GroupAssignment, Project, Role, Scope, System, User, UserAssignment}

final class Roles[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Role, Role.Create, Role.Update](baseUri, "role", authToken)
  with UniqueWithinDomain[F, Role] {

  /**
    * @param name filters the response by a role name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of roles filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): Stream[F, Role] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_id" -> domainId,
    )))
  
  override def create(create: Role.Create, extraHeaders: Header*): F[Role] = createHandleConflict(create, extraHeaders:_*) {
    def updateIt(existingRole: Role): F[Role] = {
      // Description is the only field that can be different
      if (existingRole.description != create.description) {
        update(existingRole.id, Role.Update(description = create.description), extraHeaders:_*)
      } else {
        Sync[F].pure(existingRole)
      }
    }

    create.domainId match {
      case Some(domainId) =>
        // We got a Conflict and we have a domainId so we can find the existing Role since it must already exist
        apply(create.name, domainId).flatMap(updateIt)
      case None =>
        // Currently Keystone does not accept the limit query param but it might in the future.
        // We only need 2 results to disambiguate whether the role name is unique or not.
        list(Query.fromPairs("name" -> create.name, "limit" -> "2"), extraHeaders:_*).compile.toList.flatMap { roles =>
          if (roles.lengthIs == 1) {
            updateIt(roles.head)
          } else {
            // There is more than one role with name `create.name`. We do not have enough information to disambiguate between them.
            Sync[F].raiseError(UnexpectedStatus(Conflict))
          }
        }
    }
  }
  
  /** Allows performing role assignment operations on the domain with `id` */
  def onDomain(id: String): RoleAssignment[F] =
    new RoleAssignment(baseUri, Scope.Domain.id(id), authToken)
  /** Allows performing role assignment operations on the project with `id` */
  def onProject(id: String): RoleAssignment[F] =
    new RoleAssignment(baseUri, Scope.Project(id), authToken)
  
  /** Allows performing role assignment operations on `domain`. */
  def on(domain: Domain): RoleAssignment[F] = onDomain(domain.id)
  /** Allows performing role assignment operations on `project`. */
  def on(project: Project): RoleAssignment[F] = onDomain(project.id)
  /** Allows performing role assignment operations on system. */
  def on(@nowarn system: System.type): RoleAssignment[F] =
    new RoleAssignment(baseUri, Scope.System(), authToken)
  
  def listAssignments(userId: Option[String] = None, groupId: Option[String] = None, roleId: Option[String] = None,
    domainId: Option[String] = None, projectId: Option[String] = None, system: Option[Boolean] = None,
    effective: Boolean = false, includeNames: Boolean = false, includeSubtree: Boolean = false): Stream[F, Assignment] = {
    val query: Vector[(String, Option[String])] = Vector(
      "user.id" -> userId,
      "group.id" -> groupId,
      "role.id" -> roleId,
      "scope.domain.id" -> domainId,
      "scope.project.id" -> projectId,
      "scope.system" -> system.map(_.toString),
    ).filter { case (_, value) => value.isDefined } ++
      Option.when(effective)("effective" -> Option.empty) ++
      Option.when(includeNames)("include_names" -> Option.empty) ++
      Option.when(includeSubtree)("include_subtree" -> Option.empty)
    
    list[Assignment]("role_assignments", baseUri / "role_assignments", Query.fromVector(query))
  }
  
  def listAssignmentsForUser(id: String): Stream[F, UserAssignment] =
    listAssignments(userId = Some(id)).asInstanceOf[Stream[F, UserAssignment]]
  def listAssignmentsForGroup(id: String): Stream[F, GroupAssignment] =
    listAssignments(groupId = Some(id)).asInstanceOf[Stream[F, GroupAssignment]]
  def listAssignmentsForRole(id: String): Stream[F, Assignment] = listAssignments(roleId = Some(id))
  def listAssignments(user: User): Stream[F, UserAssignment] = listAssignmentsForUser(user.id)
  def listAssignments(group: Group): Stream[F, GroupAssignment] = listAssignmentsForGroup(group.id)
  def listAssignments(role: Role): Stream[F, Assignment] = listAssignmentsForRole(role.id)
}