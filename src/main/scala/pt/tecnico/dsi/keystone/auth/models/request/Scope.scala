package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Scope {
  implicit val encoder: Encoder[Scope] = deriveEncoder(renaming.snakeCase, None)
}
case class Scope()


