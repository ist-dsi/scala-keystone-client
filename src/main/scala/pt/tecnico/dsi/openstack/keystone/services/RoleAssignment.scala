package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Method.{HEAD, PUT}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.{Assignment, Group, Role, Scope, User}

// Dotty union types would make this file simpler

class RoleAssignment[F[_]: Sync: Client] private[services] (baseUri: Uri, scope: Scope, authToken: Header)
  extends Service[F](authToken) { self =>
  import dsl._
  
  val roleAssignmentsApiQuery: Query = scope match {
    case Scope.Domain(id, _) => Query.fromPairs("scope.domain.id" -> id)
    case Scope.Project(id, _, _) => Query.fromPairs("scope.project.id" -> id)
    case Scope.System(all) => Query.fromPairs("scope.system" -> all.toString) // All is being used but it is irrelevant
    case Scope.Unscoped => Query.empty
  }
  
  /**
   * Lists all role assignments on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   keystone.roles on domain listAssignments()
   * }}}
   */
  def listAssignments(): Stream[F, Assignment] =
    self.list[Assignment]("role_assignments", baseUri / "role_assignments", roleAssignmentsApiQuery)
  
  def rolesApiUri(subjectType: String, subjectId: String): Uri = {
    val baseWithScope = scope match {
      case Scope.Domain(id, _) => baseUri / "domains" / id
      case Scope.Project(id, _, _) => baseUri / "projects" / id
      case Scope.System(_) => baseUri / "system"
      case Scope.Unscoped =>
        // This will never happen since the RoleAssignment constructor is private
        throw new IllegalArgumentException("The roles API does not support unscoped scope!")
    }
    baseWithScope / subjectType / subjectId / "roles"
  }
  
  protected def listAssignmentsFor(subjectType: String, subjectId: String): Stream[F, Role] =
    self.list[Role]("roles", rolesApiUri(subjectType, subjectId), Query.empty)
  
  /**
    * Lists the role assignments for the user with `id` on the given context.
    * @example
    * {{{
    *   val domain: Domain = ???
    *   keystone.roles on domain listAssignmentsFor "user-id"
    * }}}
    */
  def listAssignmentsForUser(id: String): Stream[F, Role] = listAssignmentsFor("users", id)
  /**
    * Lists the role assignments for the group with `id` on the given context.
    * @example
    * {{{
    *   val domain: Domain = ???
    *   keystone.roles on domain listAssignmentsFor "group-id"
    * }}}
    */
  def listAssignmentsForGroup(id: String): Stream[F, Role] = listAssignmentsFor("groups", id)
  
  /**
    * Lists the role assignments for `user` on the given context.
    * @example
    * {{{
    *   val domain: Domain = ???
    *   var user: User = ???
    *   keystone.roles on domain listAssignmentsFor user
    * }}}
    */
  def listAssignmentsFor(user: User): Stream[F, Role] = listAssignmentsForUser(user.id)
  /**
    * Lists the role assignments for `group` on the given context.
    * @example
    * {{{
    *   val domain: Domain = ???
    *   var group: Group = ???
    *   keystone.roles on domain listAssignmentsFor group
    * }}}
    */
  def listAssignmentsFor(group: Group): Stream[F, Role] = listAssignmentsForGroup(group.id)
  
  final class Assign(roleId: String) {
    private def to(subjectType: String, subjectId: String): F[Unit] =
      client.expect(PUT(rolesApiUri(subjectType, subjectId) / roleId, authToken))
    
    def toUser(id: String): F[Unit] = to("users", id)
    def toGroup(id: String): F[Unit] = to("groups", id)
    
    def to(user: User): F[Unit] = toUser(user.id)
    def to(group: Group): F[Unit] = toGroup(group.id)
  }
  /**
   * Allows assigning the role with `roleId` to user/group on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   var user: User = ???
   *   keystone.roles on domain assign "role-id" to user
   * }}}
   */
  def assign(roleId: String): Assign = new Assign(roleId)
  /**
   * Allows assigning `role` to user/group on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   val role: Role = ???
   *   var user: User = ???
   *   keystone.roles on domain assign role to user
   * }}}
   */
  def assign(role: Role): Assign = assign(role.id)
  
  final class Unassign(roleId: String) {
    private def from(subjectType: String, subjectId: String): F[Unit] =
      self.delete(rolesApiUri(subjectType, subjectId) / roleId)
    def fromUser(id: String): F[Unit] = from("users", id)
    def fromGroup(id: String): F[Unit] = from("groups", id)
    
    def from(user: User): F[Unit] = fromUser(user.id)
    def from(group: Group): F[Unit] = fromGroup(group.id)
  }
  /**
   * Allows unassigning the role with `roleId` from user/group on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   var user: User = ???
   *   keystone.roles on domain unassign "role-id" from user
   * }}}
   */
  def unassign(roleId: String): Unassign = new Unassign(roleId)
  /**
   * Allows unassigning `role` to user/group on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   val role: Role = ???
   *   var user: User = ???
   *   keystone.roles on domain unassign role from user
   * }}}
   */
  def unassign(role: Role): Unassign = unassign(role.id)
  
  final class Is(roleId: String) {
    private def assignedTo(subjectType: String, subjectId: String): F[Boolean] =
      HEAD(rolesApiUri(subjectType, subjectId) / roleId, authToken).flatMap(client.expectOption(_)(void)).map(_.isDefined)
    
    def assignedToUser(id: String): F[Boolean] = assignedTo("users", id)
    def assignedToGroup(id: String): F[Boolean] = assignedTo("groups", id)
    
    def assignedTo(user: User): F[Boolean] = assignedToUser(user.id)
    def assignedTo(group: Group): F[Boolean] = assignedToGroup(group.id)
  }
  /**
   * Allows checking if the role with `roleId` is assigned to user/group on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   var user: User = ???
   *   keystone.roles on domain is "role-id" assignedTo user
   * }}}
   */
  def is(roleId: String): Is = new Is(roleId)
  /**
   * Allows checking if `role` is assigned to user/group on the given context.
   * @example
   * {{{
   *   val domain: Domain = ???
   *   val role: Role = ???
   *   var user: User = ???
   *   keystone.roles on domain is role assignedTo user
   * }}}
   */
  def is(role: Role): Is = is(role.id)
}