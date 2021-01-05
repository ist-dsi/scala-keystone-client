package pt.tecnico.dsi.openstack.keystone.models

import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.RoleAssignment

/**
 * @define scope scope
 */
trait RoleAssigner {
  /** The role assignment for $scope */
  def roleAssignment[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F]
  
  /**
   * Lists the role assignments for the user with `id` on $scope.
   * @example
   * {{{
   *   $scope listAssignmentsFor "user-id"
   * }}}
   */
  def listAssignmentsForUser[F[_]: KeystoneClient](id: String): F[List[Role]] = roleAssignment.listAssignmentsForUser(id)
  /**
   * Lists the role assignments for the group with `id` on $scope.
   * @example
   * {{{
   *   $scope listAssignmentsFor "group-id"
   * }}}
   */
  def listAssignmentsForGroup[F[_]: KeystoneClient](id: String): F[List[Role]] = roleAssignment.listAssignmentsForGroup(id)
  
  /**
   * Lists the role assignments for `user` on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   $scope listAssignmentsFor user
   * }}}
   */
  def listAssignmentsFor[F[_]: KeystoneClient](user: User): F[List[Role]] = listAssignmentsForUser(user.id)
  /**
   * Lists the role assignments for `group` on $scope.
   * @example
   * {{{
   *   var group: Group = ???
   *   $scope listAssignmentsFor group
   * }}}
   */
  def listAssignmentsFor[F[_]: KeystoneClient](group: Group): F[List[Role]] = listAssignmentsForGroup(group.id)

  /**
   * Allows assigning the role with `roleId` to user/group on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   $scope assign "role-id" to user
   * }}}
   */
  def assign[F[_]: KeystoneClient](roleId: String): RoleAssignment[F]#Assign = {
    val rs = roleAssignment
    new rs.Assign(roleId)
  }
  /**
   * Allows assigning the `role` to user/group on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   var role: Role = ???
   *   $scope assign role to user
   * }}}
   */
  def assign[F[_]: KeystoneClient](role: Role): RoleAssignment[F]#Assign = assign(role.id)
  
  /**
   * Allows unassigning the role with `roleId` to user/group on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   $scope unassign "role-id" to user
   * }}}
   */
  def unassign[F[_]: KeystoneClient](roleId: String): RoleAssignment[F]#Unassign = {
    val rs = roleAssignment
    new rs.Unassign(roleId)
  }
  /**
   * Allows unassigning the `role` from user/group on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   var role: Role = ???
   *   $scope unassign role from user
   * }}}
   */
  def unassign[F[_]: KeystoneClient](role: Role): RoleAssignment[F]#Unassign = unassign(role.id)
  
  /**
   * Allows checking if the role with `roleId` is assigned to user/group on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   $scope is "role-id" assignedTo user
   * }}}
   */
  def is[F[_]: KeystoneClient](roleId: String): RoleAssignment[F]#Is = {
    val rs = roleAssignment
    new rs.Is(roleId)
  }
  /**
   * Allows checking if `role` is assigned to user/group on $scope.
   * @example
   * {{{
   *   var user: User = ???
   *   var role: Role = ???
   *   $scope is role assignedTo user
   * }}}
   */
  def is[F[_]: KeystoneClient](role: Role): RoleAssignment[F]#Is = is(role.id)
}