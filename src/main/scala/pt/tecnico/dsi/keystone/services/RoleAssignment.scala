package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.Status.{NotFound, Successful}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dsl.impl.Methods
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.keystone.models.{Role, WithId}

trait RoleAssignment[F[_]] { this: CRUDService[F, _] =>
  object roles {
    val users = new GenericRoleAssignmentService(uri, "users", authToken)
    val groups = new GenericRoleAssignmentService(uri, "groups", authToken)
  }
}

class GenericRoleAssignmentService[F[_]]
  (uri: Uri, target: String, authToken: Header)
  (implicit client: Client[F], F: Sync[F]) {

  val genericListEndpoint: GenericListEndpoint[F] = ListEndpoint[F](authToken)
  val dsl = new Http4sClientDsl[F] with Methods
  import dsl._

  def list(id: String, targetId: String): Stream[F, WithId[Role]] =
    genericListEndpoint[WithId[Role]]("roles", uri / id / target / targetId / "roles")

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

  def delete(id: String, groupId: String, roleId: String): F[Unit] =
    client.fetch(DELETE(uri / id / target / groupId / "roles" / roleId, authToken)) {
      case Successful(_) | NotFound(_) => F.pure(())
      case response => F.raiseError(UnexpectedStatus(response.status))
    }
}