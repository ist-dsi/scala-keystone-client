package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Region {
  implicit val codec: Codec.AsObject[Region] = deriveCodec(renaming.snakeCase, false, None)

  def apply(description: String, parentRegionId: String): Region = Region(description, Some(parentRegionId))
  def apply(description: String): Region = Region(description, None)
}

case class Region private[keystone] (
  description: String,
  parentRegionId: Option[String],
)

