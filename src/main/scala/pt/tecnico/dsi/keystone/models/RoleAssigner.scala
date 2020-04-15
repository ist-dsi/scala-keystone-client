package pt.tecnico.dsi.keystone.models

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.services.{RoleAssignment, RoleAssignmentService}

trait RoleAssigner[T] { self: IdFetcher[T] =>
  /** Returns the role assignment service responsible for managing roles. */
  def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F]

  /** Contains role assignment services that pertain to the client object. */
  object roles {
    def users[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, T](self, service.roles.users)
    def groups[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, T](self, service.roles.groups)
  }
}

class ContextualRoleAssignmentService[F[_]: Sync: KeystoneClient, T](idOperations: IdFetcher[T], roleAssignmentService: RoleAssignmentService[F]) {
  import idOperations._
  // TODO: List is missing

  def list(targetId: String): Stream[F, WithId[Role]] = withId(o => roleAssignmentService.list(o.id, targetId))

  /**
    * @see [[RoleAssignmentService.check]]
    */
  def check(targetId: String, roleId: String): F[Boolean] = withId(o => roleAssignmentService.check(o.id, targetId, roleId))

  /**
    * @see [[RoleAssignmentService.assign]]
    */
  def assign(targetId: String, roleId: String): F[Unit] = {
    idOperations.map(_.id).flatMap { id =>
      roleAssignmentService.assign(id, targetId, roleId)
    }
  }

  /**
    * @see [[RoleAssignmentService.delete]]
    */
  def delete(targetId: String, roleId: String): F[Unit] = {
    idOperations.map(_.id).flatMap { id =>
      roleAssignmentService.delete(id, targetId, roleId)
    }
  }
}