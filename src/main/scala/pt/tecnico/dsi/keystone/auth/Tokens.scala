package pt.tecnico.dsi.keystone.auth

import cats.effect.Sync
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.Status.Successful
import org.http4s.syntax.string._
import pt.tecnico.dsi.keystone.auth.models.request.AuthTokenRequest
import pt.tecnico.dsi.keystone.auth.models.response.AuthTokenResponse
import cats.syntax.functor._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods

class Tokens[F[_]: Sync](uri: Uri)(implicit client: Client[F]) {

	private val dsl = new Http4sClientDsl[F] with Methods {}
	import dsl._

	/**
	 * Password authentication with unscoped authorization.
	 */
	def authenticate(authTokenRequest: AuthTokenRequest): F[(AuthTokenResponse, String)] = {
		client.fetch(POST(authTokenRequest, uri)) {
			case Successful(response) =>
				val header = response.headers.get("X-Subject-Token".ci)
				response
					.as[AuthTokenResponse]
					.map(authResponse => (authResponse, header.get.value))
		}
	}

}
