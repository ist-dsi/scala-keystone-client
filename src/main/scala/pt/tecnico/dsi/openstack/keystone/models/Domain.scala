package pt.tecnico.dsi.openstack.keystone.models

import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.{ConfiguredCodec, ConfiguredDecoder, ConfiguredEncoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.RoleAssignment

object Domain:
  /**
   * Options to create a Domain.
   *
   * @param name The name of the domain.
   * @param description The description of the domain.
   * @param enabled          If set to true, domain is created enabled. If set to false, domain is created disabled.
   *                         Users can only authorize against an enabled domain (and any of its projects). In addition, users can only
   *                         authenticate if the domain that owns them is also enabled. Disabling a domain prevents both of these things.
   * @param explicitDomainId The ID of the domain. A domain created this way will not use an auto-generated ID, but will use the ID passed in instead.
   *                         Identifiers passed in this way must conform to the existing ID generation scheme: UUID4 without dashes.
   */
  case class Create(
    name: String,
    description: String = "",
    enabled: Boolean = true,
    explicitDomainId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty
  
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
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description, enabled).exists(_.isDefined)
/**
 * @define scope domain
 */
final case class Domain(
  id: String,
  name: String,
  enabled: Boolean,
  description: String,
  links: List[Link] = List.empty,
) extends Identifiable with RoleAssigner derives ConfiguredCodec, ShowPretty:
  def roleAssignment[F[_]](using client: KeystoneClient[F]): RoleAssignment[F] = client.roles.on(this)
