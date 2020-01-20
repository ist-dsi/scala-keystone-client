package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation._

object AuthTokenRequest {
	implicit val encoder: Encoder[AuthTokenRequest] = deriveEncoder(renaming.snakeCase, None)
}

case class AuthTokenRequest (
	auth: Auth
)

