package pt.tecnico.dsi.keystone.models.auth.response

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object AuthTokenResponse {
	implicit val decoder: Decoder[AuthTokenResponse] = deriveDecoder(renaming.snakeCase, false, None)
}

case class AuthTokenResponse (
	token: Token
)

