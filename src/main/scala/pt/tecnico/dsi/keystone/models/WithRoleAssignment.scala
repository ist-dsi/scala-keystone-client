package pt.tecnico.dsi.keystone.models

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.services.{RoleAssignment, RoleAssignmentService}

trait WithRoleAssignment[T] {
  /**
    * Returns the role assignment service responsible for managing roles.
    */
  def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F]

  /**
    * Returns an instance of a client object with a certain id.
    */
  def withId[F[_]](implicit client: KeystoneClient[F]): F[WithId[T]]

  /**
    * Contains role assignment services that pertain to the client object.
    */
  object roles {
    def users[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, T](withId, service.roles.users)
    def groups[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, T](withId, service.roles.groups)
  }
}

class ContextualRoleAssignmentService[F[_]: Sync, T](withId: F[WithId[T]], roleAssignmentService: RoleAssignmentService[F]) {
  // TODO: List is missing

  /**
    * @see [[RoleAssignmentService.check]]
    */
  def check(targetId: String, roleId: String): F[Boolean] = {
    withId.map(_.id).flatMap { id =>
      roleAssignmentService.check(id, targetId, roleId)
    }
  }

  /**
    * @see [[RoleAssignmentService.assign]]
    */
  def assign(targetId: String, roleId: String): F[Unit] = {
    withId.map(_.id).flatMap { id =>
      roleAssignmentService.assign(id, targetId, roleId)
    }
  }

  /**
    * @see [[RoleAssignmentService.delete]]
    */
  def delete(targetId: String, roleId: String): F[Unit] = {
    withId.map(_.id).flatMap { id =>
      roleAssignmentService.delete(id, targetId, roleId)
    }
  }
}