package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object System {
  implicit val encoder: Encoder[System] = deriveEncoder(renaming.snakeCase, None)
}

case class System(all: Boolean)