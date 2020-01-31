package pt.tecnico.dsi.keystone.models.auth.response

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object User {
	implicit val decoder: Decoder[User] = deriveDecoder(renaming.snakeCase, false, None)
}

case class User (
	domain: Domain,
	id: String,
	name: String,
	passwordExpiresAt: Option[String]
)