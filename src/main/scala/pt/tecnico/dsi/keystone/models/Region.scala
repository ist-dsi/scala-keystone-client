package pt.tecnico.dsi.keystone.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Region {
  implicit val decoder: Decoder[Region] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Region] = deriveEncoder(renaming.snakeCase, None)

  def apply(description: String, parentRegionId: String): Region = Region(description, Some(parentRegionId))
  def apply(description: String): Region = Region(description, None)

}

case class Region private[keystone] (
  description: String,
  parentRegionId: Option[String],
)

