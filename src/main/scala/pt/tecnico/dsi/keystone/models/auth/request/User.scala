package pt.tecnico.dsi.keystone.models.auth.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object User {
	implicit val encoder: Encoder[User] = deriveEncoder[User](renaming.snakeCase, None)
}

case class User (
	id: Option[String],
	name: String,
	domain: Domain,
	password: String
)