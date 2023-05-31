package pt.tecnico.dsi.openstack.keystone.models

import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.{ConfiguredCodec, ConfiguredEncoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Role:
  /**
   * Options to create a Role
   *
   * @param name The role name.
   * @param description The description of the role.
   * @param domainId The ID of the domain of the role.
   */
  case class Create(
    name: String,
    description: Option[String] = None,
    domainId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty
  
  /**
   * Options to update a Role
   *
   * @param name TThe new role name.
   * @param description The new role description.
   */
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description).exists(_.isDefined)
final case class Role(
  id: String,
  name: String,
  description: Option[String],
  domainId: Option[String],
  links: List[Link] = List.empty,
) extends Identifiable derives ConfiguredCodec, ShowPretty