package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client

final class System[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends BaseService[F](authToken)
  with RoleAssignment[F] {

  override val uri: Uri = baseUri / "system"
}
