package pt.tecnico.dsi.openstack.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Role {
  implicit val decoder: Decoder[Role] = deriveDecoder(renaming.snakeCase)

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
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
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to update a Role
   *
   * @param name TThe new role name.
   * @param description The new role description.
   */
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
  )
}
final case class Role(
  id: String,
  name: String,
  description: Option[String],
  domainId: Option[String],
  links: List[Link] = List.empty,
) extends Identifiable