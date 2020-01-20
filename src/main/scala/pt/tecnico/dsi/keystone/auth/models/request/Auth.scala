package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Auth {
	implicit val encoder: Encoder[Auth] = deriveEncoder(renaming.snakeCase, None)
}

case class Auth (identity: Identity)
