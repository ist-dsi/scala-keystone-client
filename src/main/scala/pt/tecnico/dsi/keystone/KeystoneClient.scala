package pt.tecnico.dsi.keystone

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client

class KeystoneClient[F[_]: Sync](val baseUri: Uri)(implicit client: Client[F]) { self =>
  val uri = baseUri / "v1"
}