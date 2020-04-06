package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.Status.{NotFound, Successful}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.keystone.models.{Role, WithId}

trait RoleAssignment[F[_]] { this: BaseService[F] =>
  object roles {
    val users = new RoleAssignmentService(uri, "users", authToken)
    val groups = new RoleAssignmentService(uri, "groups", authToken)
  }
}

class RoleAssignmentService[F[_]: Sync: Client](val uri: Uri, target: String, authToken: Header) extends BaseService[F](authToken) {
  import dsl._

  def list(id: String, targetId: String): Stream[F, WithId[Role]] =
    genericList[WithId[Role]]("roles", uri / id / target / targetId / "roles")

  def assign(id: String, targetId: String, roleId: String): F[Unit] =
    client.fetch(PUT(uri / id / target / targetId / "roles" / roleId, authToken)) {
      case Successful(_) => F.pure(())
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  def check(id: String, targetId: String, roleId: String): F[Boolean] =
    client.fetch(HEAD(uri / id / target / targetId / "roles" / roleId, authToken)) {
      case Successful(_) => F.pure(true)
      case NotFound(_) => F.pure(false)
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  def delete(id: String, targetId: String, roleId: String): F[Unit] =
    genericDelete(uri / id / target / targetId / "roles" / roleId)
}