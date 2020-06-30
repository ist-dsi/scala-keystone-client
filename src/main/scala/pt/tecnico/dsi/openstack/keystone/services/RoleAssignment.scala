package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Method.{HEAD, PUT}
import org.http4s.Status.{NotFound, Successful}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Role

trait RoleAssignment[F[_]] { this: CrudService[F, _] =>
  object roles {
    /**
      * Role assignments for users on this context.
      */
    val users = new RoleAssignmentService(uri, "users", authToken)
    /**
      * Role assignments for groups on this context.
      */
    val groups = new RoleAssignmentService(uri, "groups", authToken)
  }
}

class RoleAssignmentService[F[_]: Sync: Client](val uri: Uri, target: String, authToken: Header) extends Service[F](authToken) {
  import dsl._

  /**
    * Lists role assignments for a target on a certain context
    * @param id the object's id
    * @param targetId the target's id
    * @return roles assigned
    */
  def list(id: String, targetId: String): Stream[F, WithId[Role]] =
    super.list[WithId[Role]]("roles", uri / id / target / targetId / "roles", Query.empty)

  /**
    * Assigns a role to a target on a certain context
    * @param id the contexts's id
    * @param targetId the target's id (group/project)
    */
  def assign(id: String, targetId: String, roleId: String): F[Unit] =
    PUT(uri / id / target / targetId / "roles" / roleId, authToken).flatMap(client.run(_).use {
      case Successful(_) => F.pure(())
      case response => F.raiseError(UnexpectedStatus(response.status))
    })

  /**
    * Checks if a certain role is assigned to a target on a context
    * @param id the context's id
    * @param targetId the target's id
    * @return whether the role is assigned
    */
  def check(id: String, targetId: String, roleId: String): F[Boolean] =
    HEAD(uri / id / target / targetId / "roles" / roleId, authToken).flatMap(client.run(_).use {
      case Successful(_) => F.pure(true)
      case NotFound(_) => F.pure(false)
      case response => F.raiseError(UnexpectedStatus(response.status))
    })

  /**
    * Unassign role from group on domain
    * @param id the context's id
    * @param targetId the target's id
    */
  def delete(id: String, targetId: String, roleId: String): F[Unit] =
    super.delete(uri / id / target / targetId / "roles" / roleId)
}