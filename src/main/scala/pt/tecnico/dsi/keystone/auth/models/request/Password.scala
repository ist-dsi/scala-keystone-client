package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Password {
	implicit val encoder: Encoder[Password] = deriveEncoder(renaming.snakeCase, None)
}

case class Password(user: User)