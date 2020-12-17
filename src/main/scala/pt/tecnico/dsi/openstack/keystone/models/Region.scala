package pt.tecnico.dsi.openstack.keystone.models

import cats.derived
import cats.derived.ShowPretty
import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Region {
  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
  /**
   * Options to create a Region.
   * @param id The ID for the region.
   * @param description The region description.
   * @param parentRegionId To make this region a child of another region, set this parameter to the ID of the parent region.
   */
  case class Create(
    id: String,
    description: String = "",
    parentRegionId: Option[String] = None,
  )
  
  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
  /**
   * Options to update a Region.
   * @param description The new region description.
   * @param parentRegionId To make this region a child of another region, set this parameter to the ID of the parent region.
   */
  case class Update(
    description: Option[String] = None,
    parentRegionId: Option[String] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(description, parentRegionId).exists(_.isDefined)
    }
  }
  
  implicit val decoder: Decoder[Region] = deriveDecoder(renaming.snakeCase)
  implicit val show: ShowPretty[Region] = derived.semiauto.showPretty
}
final case class Region(
  id: String,
  description: String,
  parentRegionId: Option[String],
  links: List[Link] = List.empty,
) extends Identifiable

