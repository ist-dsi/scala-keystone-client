package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object User {
	implicit val encoder: Encoder[User] = deriveEncoder[User](renaming.snakeCase, None)
}

case class User (name: String, domain: Domain, password: String)