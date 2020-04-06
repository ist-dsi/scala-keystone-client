package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client

class System[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F]) {
  val users: SingletonRoleAssignmentService[F] = SingletonRoleAssignmentService(
    new GenericRoleAssignmentService(baseUri / "system", "users", authToken))
  val groups: SingletonRoleAssignmentService[F] = SingletonRoleAssignmentService(
    new GenericRoleAssignmentService(baseUri / "system", "groups", authToken))
}
