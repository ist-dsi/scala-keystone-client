package pt.tecnico.dsi.keystone.auth

import cats.effect.Sync
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.Status.Successful
import pt.tecnico.dsi.keystone.auth.models.request.AuthTokenRequest
import pt.tecnico.dsi.keystone.auth.models.response.AuthTokenResponse

class Tokens[F[_]: Sync](uri: Uri)(implicit client: Client[F]) {

	/**
	 * Password authentication with unscoped authorization.
	 */
	def authenticate(authTokenRequest: AuthTokenRequest): F[AuthTokenResponse] = {
		val postRequest = Request(method = Method.POST, uri = uri).withEntity(authTokenRequest)
		client.fetch[AuthTokenResponse](postRequest) {
			case Successful(response) => response.as[AuthTokenResponse]
		}
	}

}
