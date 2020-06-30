package pt.tecnico.dsi.openstack.keystone.models

import cats.effect.Sync
import fs2.Stream
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.{RoleAssignment, RoleAssignmentService}
import pt.tecnico.dsi.openstack.common.models.WithId

trait RoleAssigner[T <: IdFetcher[T]] { self: IdFetcher[T] =>
  /** Returns the role assignment service responsible for managing roles. */
  def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F]

  /** Contains role assignment services that pertain to the client object. */
  object roles {
    def users[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, T](self, service.roles.users)
    def groups[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, T](self, service.roles.groups)
  }
}

class ContextualRoleAssignmentService[F[_]: Sync: KeystoneClient, T <: IdFetcher[T]](idOperations: IdFetcher[T], roleAssignmentService: RoleAssignmentService[F]) {
  import idOperations._

  /** @see RoleAssignmentService.list */
  def list(targetId: String): Stream[F, WithId[Role]] = withId(o => roleAssignmentService.list(o.id, targetId))

  /** @see RoleAssignmentService.check */
  def check(targetId: String, roleId: String): F[Boolean] = withId(o => roleAssignmentService.check(o.id, targetId, roleId))

  /** @see RoleAssignmentService.assign */
  def assign(targetId: String, roleId: String): F[Unit] = withId(o => roleAssignmentService.assign(o.id, targetId, roleId))

  /** @see RoleAssignmentService.delete */
  def delete(targetId: String, roleId: String): F[Unit] = withId(o => roleAssignmentService.delete(o.id, targetId, roleId))
}