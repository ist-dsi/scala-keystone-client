package pt.tecnico.dsi.openstack.keystone.models

import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.{ConfiguredCodec, ConfiguredEncoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Region:
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
  ) derives ConfiguredEncoder, ShowPretty
  
  /**
   * Options to update a Region.
   * @param description The new region description.
   * @param parentRegionId To make this region a child of another region, set this parameter to the ID of the parent region.
   */
  case class Update(
    description: Option[String] = None,
    parentRegionId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(description, parentRegionId).exists(_.isDefined)
final case class Region(
  id: String,
  description: String,
  parentRegionId: Option[String],
  links: List[Link] = List.empty,
) extends Identifiable derives ConfiguredCodec, ShowPretty

