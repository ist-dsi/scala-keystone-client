package pt.tecnico.dsi.keystone

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client

class KeystoneClient[F[_]: Sync](val baseUri: Uri)(implicit client: Client[F]) { self =>
	val uri: Uri = baseUri / "v3"

	/**
	 * Authentication and token management.
	 */
	object auth {
		import pt.tecnico.dsi.keystone.auth._

		val uri: Uri = self.uri / "auth"
		val tokens = new Tokens[F](uri / "tokens")
	}
}