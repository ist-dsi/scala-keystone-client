package pt.tecnico.dsi.keystone

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.auth.Tokens
import pt.tecnico.dsi.keystone.auth.models.request.AuthTokenRequest
import pt.tecnico.dsi.keystone.auth.models.response.Token
import pt.tecnico.dsi.keystone.domains.Domains

object KeystoneClient {

	/**
		* Create an authenticated `KeystoneClient`.
		*/
	def create[F[_]: Sync](baseUri: Uri, auth: AuthTokenRequest)(implicit client: Client[F]): F[KeystoneClient[F]] = {
		val token = new Tokens[F](baseUri.withPath("/v3/auth/tokens"))
		for {
			(response, header) <- token.authenticate(auth)
		} yield new KeystoneClient(baseUri, response.token,  Header("X-Auth-Token", header))
	}
}

class KeystoneClient[F[_]: Sync](val baseUri: Uri, val authenticatedUser: Token, val token: Header)(implicit client: Client[F]) { self =>

	val uri: Uri = baseUri / "v3"
	val domains = new Domains[F](uri / "domains", token)

}



