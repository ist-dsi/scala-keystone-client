package pt.tecnico.dsi.openstack.keystone.models

import io.circe.{Codec, Encoder}
import io.circe.derivation.{deriveCodec, deriveEncoder, renaming}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}

object Service {
  implicit val codec: Codec.AsObject[Service] = deriveCodec(renaming.snakeCase)

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to create a Service.
   *
   * @param name The service name.
   * @param type The service type, which describes the API implemented by the service.
   * @param description The service description.
   * @param enabled Defines whether the service and its endpoints appear in the service catalog.
   */
  case class Create(
    name: String,
    `type`: String,
    description: Option[String] = None,
    enabled: Boolean = true,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to update a Service.
   *
   * @param name The new name of the service.
   * @param type The new service type, which describes the API implemented by the service.
   * @param description The new description of the service.
   * @param enabled Defines whether the service and its endpoints appear in the service catalog.
   */
  case class Update(
    name: Option[String] = None,
    `type`: Option[String] = None,
    description: Option[String] = None,
    enabled: Option[Boolean] = None,
  )
}

final case class Service private[keystone] (
  id: String,
  name: String,
  `type`: String,
  description: Option[String],
  enabled: Boolean,
  links: List[Link] = List.empty,
) extends Identifiable