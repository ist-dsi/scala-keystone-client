package pt.tecnico.dsi.keystone.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Region {
  implicit val decoder: Decoder[Region] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Region] = deriveEncoder(renaming.snakeCase, None)
}
case class Region(
  description: String,
  parentRegionId: String,
)

