package pt.tecnico.dsi.keystone

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.auth.Tokens
import pt.tecnico.dsi.keystone.auth.models.request.AuthTokenRequest
import pt.tecnico.dsi.keystone.auth.models.response.Token

object KeystoneClient {
	def create[F[_]: Sync](baseUri: Uri, auth: AuthTokenRequest)(implicit client: Client[F]): F[KeystoneClient[F]] = {
		val token = new Tokens[F](baseUri / "v3" / "auth" / "tokens")
		token.authenticate(auth).map(response => new KeystoneClient(baseUri, Header("X-Subject-Token", "Dummy"), response.token))
	}
}

class KeystoneClient[F[_]: Sync](val baseUri: Uri, val token: Header, val authenticatedUser: Token)(implicit client: Client[F]) { self =>
	val uri: Uri = baseUri / "v3"

	/**
	 * Authentication and token management.
	 */
	object auth {
		import pt.tecnico.dsi.keystone.auth._

		val uri: Uri = self.uri / "auth"
		val tokens = new Tokens[F](uri / "tokens")
		//val domains = new Domains[F](uri / "domains")
		//val groups = new Groups[F](uri / "groups")
	}
}



