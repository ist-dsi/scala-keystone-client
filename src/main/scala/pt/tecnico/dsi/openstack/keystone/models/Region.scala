package pt.tecnico.dsi.openstack.keystone.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Region {
  implicit val decoder: Decoder[Region] = deriveDecoder(renaming.snakeCase)

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to create a Region.
   * @param id The ID for the region.
   * @param description The region description.
   * @param parentRegionId To make this region a child of another region, set this parameter to the ID of the parent region.
   */
  case class Create(
    id: Option[String] = None,
    description: Option[String] = None,
    parentRegionId: Option[String] = None,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to update a Region.
   * @param description The new region description.
   * @param parentRegionId To make this region a child of another region, set this parameter to the ID of the parent region.
   */
  case class Update(
    description: Option[String] = None,
    parentRegionId: Option[String] = None,
  )
}
final case class Region(
  id: String,
  description: Option[String],
  parentRegionId: Option[String],
  links: List[Link] = List.empty,
) extends Identifiable

