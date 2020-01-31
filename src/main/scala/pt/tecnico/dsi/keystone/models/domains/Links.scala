package pt.tecnico.dsi.keystone.models.domains

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Links {
  implicit val decoder: Decoder[Links] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Links] = deriveEncoder(renaming.snakeCase, None)
}

case class Links(
  self: Option[String],
  next: Option[String],
  previous: Option[String]
)