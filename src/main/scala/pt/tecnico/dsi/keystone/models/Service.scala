package pt.tecnico.dsi.keystone.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Service {
  implicit val decoder: Decoder[Service] = deriveDecoder(renaming.snakeCase, true, None)
  implicit val encoder: Encoder[Service] = deriveEncoder(renaming.snakeCase, None)
}
case class Service(
  `type`: String,
  name: String,
  description: String,
  enabled: Boolean = true,
)