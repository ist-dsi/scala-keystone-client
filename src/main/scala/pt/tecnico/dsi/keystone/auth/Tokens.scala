package pt.tecnico.dsi.keystone.auth

import cats.effect.Sync
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import org.http4s.Status.Successful
import org.http4s.syntax.string._
import pt.tecnico.dsi.keystone.auth.models.request.AuthTokenRequest
import pt.tecnico.dsi.keystone.auth.models.response.AuthTokenResponse
import cats.syntax.functor._

class Tokens[F[_]: Sync](uri: Uri)(implicit client: Client[F]) {

	/**
	 * Password authentication with unscoped authorization.
	 */
	def authenticate(authTokenRequest: AuthTokenRequest): F[(AuthTokenResponse, String)] = {
		val postRequest = Request(method = Method.POST, uri = uri).withEntity(authTokenRequest)
		client.fetch(postRequest) {
			case Successful(response) =>
				val header = response.headers.get("X-Subject-Token".ci)
				response
					.as[AuthTokenResponse]
					.map(authResponse => (authResponse, header.get.value))
		}
	}

}
