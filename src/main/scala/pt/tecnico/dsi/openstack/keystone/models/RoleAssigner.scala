package pt.tecnico.dsi.openstack.keystone.models

import cats.effect.Sync
import fs2.Stream
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.{RoleAssignment, RoleAssignmentService}

trait RoleAssigner {
  /** Returns the role assignment service responsible for managing roles. */
  def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F]

  def id: String

  /** Contains role assignment services that pertain to the client object. */
  object roles {
    def users[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F](id, service.roles.users)
    def groups[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F](id, service.roles.groups)
  }
}

class ContextualRoleAssignmentService[F[_]: Sync: KeystoneClient](id: String, roleAssignmentService: RoleAssignmentService[F]) {
  /** @see RoleAssignmentService.list */
  def list(targetId: String): Stream[F, Role] = roleAssignmentService.list(id, targetId)

  /** @see RoleAssignmentService.check */
  def check(targetId: String, roleId: String): F[Boolean] = roleAssignmentService.check(id, targetId, roleId)

  /** @see RoleAssignmentService.assign */
  def assign(targetId: String, roleId: String): F[Unit] = roleAssignmentService.assign(id, targetId, roleId)

  /** @see RoleAssignmentService.delete */
  def delete(targetId: String, roleId: String): F[Unit] = roleAssignmentService.delete(id, targetId, roleId)
}