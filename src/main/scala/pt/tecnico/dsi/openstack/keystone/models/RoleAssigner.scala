package pt.tecnico.dsi.openstack.keystone.models

import fs2.Stream
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.RoleAssignment

/**
 * @define context
 */
trait RoleAssigner {
  /** The role assignment for $context */
  def roleAssignment[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F]
  
  /**
   * Lists the role assignments for the user with `id` on $context.
   * @example
   * {{{
   *   $context listAssignmentsFor "user-id"
   * }}}
   */
  def listAssignmentsForUser[F[_]: KeystoneClient](id: String): Stream[F, Role] = roleAssignment.listAssignmentsForUser(id)
  /**
   * Lists the role assignments for the group with `id` on $context.
   * @example
   * {{{
   *   $context listAssignmentsFor "group-id"
   * }}}
   */
  def listAssignmentsForGroup[F[_]: KeystoneClient](id: String): Stream[F, Role] = roleAssignment.listAssignmentsForGroup(id)
  /**
   * Lists the role assignments for `user` on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   $context listAssignmentsFor user
   * }}}
   */
  def listAssignmentsFor[F[_]: KeystoneClient](user: User): Stream[F, Role] = listAssignmentsFor(user)
  /**
   * Lists the role assignments for `group` on $context.
   * @example
   * {{{
   *   var group: Group = ???
   *   $context listAssignmentsFor group
   * }}}
   */
  def listAssignmentsFor[F[_]: KeystoneClient](group: Group): Stream[F, Role] = listAssignmentsFor(group)
  
  /**
   * Allows assigning the role with `roleId` to user/group on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   $context assign "role-id" to user
   * }}}
   */
  def assign[F[_]: KeystoneClient](roleId: String): RoleAssignment[F]#Assign = {
    val rs = roleAssignment
    new rs.Assign(roleId)
  }
  /**
   * Allows assigning the `role` to user/group on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   var role: Role = ???
   *   $context assign role to user
   * }}}
   */
  def assign[F[_]: KeystoneClient](role: Role): RoleAssignment[F]#Assign = assign(role.id)
  
  /**
   * Allows unassigning the role with `roleId` to user/group on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   $context unassign "role-id" to user
   * }}}
   */
  def unassign[F[_]: KeystoneClient](roleId: String): RoleAssignment[F]#Unassign = {
    val rs = roleAssignment
    new rs.Unassign(roleId)
  }
  /**
   * Allows unassigning the `role` from user/group on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   var role: Role = ???
   *   $context unassign role from user
   * }}}
   */
  def unassign[F[_]: KeystoneClient](role: Role): RoleAssignment[F]#Unassign = unassign(role.id)
  
  /**
   * Allows checking if the role with `roleId` is assigned to user/group on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   $context is "role-id" assignedTo user
   * }}}
   */
  def is[F[_]: KeystoneClient](roleId: String): RoleAssignment[F]#Is = {
    val rs = roleAssignment
    new rs.Is(roleId)
  }
  /**
   * Allows checking if `role` is assigned to user/group on $context.
   * @example
   * {{{
   *   var user: User = ???
   *   var role: Role = ???
   *   $context is role assignedTo user
   * }}}
   */
  def is[F[_]: KeystoneClient](role: Role): RoleAssignment[F]#Is = is(role.id)
}