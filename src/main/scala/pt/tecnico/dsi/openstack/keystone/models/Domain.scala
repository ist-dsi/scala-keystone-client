package pt.tecnico.dsi.openstack.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.RoleAssignment

object Domain {
  implicit val decoder: Decoder[Domain] = deriveDecoder(renaming.snakeCase)

  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to create a Domain.
   *
   * @param name The name of the domain.
   * @param description The description of the domain.
   * @param explicitDomainId The ID of the domain. A domain created this way will not use an auto-generated ID, but will use the ID passed in instead.
   *                         Identifiers passed in this way must conform to the existing ID generation scheme: UUID4 without dashes.
   * @param enabled          If set to true, domain is created enabled. If set to false, domain is created disabled.
   *                         Users can only authorize against an enabled domain (and any of its projects). In addition, users can only
   *                         authenticate if the domain that owns them is also enabled. Disabling a domain prevents both of these things.
   */
  case class Create(
    name: String,
    description: Option[String] = None,
    explicitDomainId: Option[String] = None,
    enabled: Boolean = true,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
  }
  /**
   * Options to update a Domain.
   *
   * @param name The new name of the domain.
   * @param description The new description of the domain.
   * @param enabled If set to true, domain is created enabled. If set to false, domain is created disabled.
   *                Users can only authorize against an enabled domain (and any of its projects). In addition, users can only
   *                authenticate if the domain that owns them is also enabled. Disabling a domain prevents both of these things.
   */
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
    enabled: Option[Boolean] = None,
  )
}

/**
 * @define context domain
 */
final case class Domain(
  id: String,
  name: String,
  enabled: Boolean,
  description: Option[String] = None,
  links: List[Link] = List.empty,
) extends Identifiable with RoleAssigner {
  def roleAssignment[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F] = client.roles.on(this)
}
