package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Token {
  implicit val encoder: Encoder[Token] = deriveEncoder(renaming.snakeCase, None)
}
case class Token(token: String)


