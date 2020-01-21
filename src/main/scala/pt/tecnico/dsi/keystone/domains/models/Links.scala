package pt.tecnico.dsi.keystone.domains.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, renaming, deriveEncoder}

object Links {
  implicit val decoder: Decoder[Links] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Links] = deriveEncoder(renaming.snakeCase, None)
}

case class Links(
  self: Option[String],
  next: Option[String],
  previous: Option[String]
)