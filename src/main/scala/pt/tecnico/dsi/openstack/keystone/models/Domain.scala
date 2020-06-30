package pt.tecnico.dsi.openstack.keystone.models

import cats.effect.Sync
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.{Domains, RoleAssignment}
import pt.tecnico.dsi.openstack.common.models.WithId

object Domain {
  implicit val codec: Codec.AsObject[Domain] = deriveCodec(renaming.snakeCase)

  def apply(name: String, enabled: Boolean, description: String): Domain = Domain(name, enabled, description, Seq.empty)

  implicit class WithIdDomainExtensions[H[_]](domain: WithId[Domain])(implicit client: KeystoneClient[H], H: Sync[H])
    extends IdFetcher[Domain] with RoleAssigner[Domain] {

    override def getWithId[F[_]: Sync](implicit client: KeystoneClient[F]): F[WithId[Domain]] = implicitly(Sync[F]).pure(domain)
    override def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F] = domain.service
  }
}

case class Domain(
  name: String,
  enabled: Boolean,
  description: String,
  tags: Seq[String]
) extends Enabler[Domain] with IdFetcher[Domain] with RoleAssigner[Domain] {
  override def withEnabled(enabled: Boolean): Domain = copy(enabled = enabled)

  override def getWithId[F[_]: Sync](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.getByName(name)

  override def service[F[_]](implicit client: KeystoneClient[F]): Domains[F] = client.domains
}

