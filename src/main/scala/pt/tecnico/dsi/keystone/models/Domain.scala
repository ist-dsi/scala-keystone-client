package pt.tecnico.dsi.keystone.models

import cats.effect.Sync
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.services.{Domains, RoleAssignment}

object Domain {
  implicit val codec: Codec.AsObject[Domain] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, enabled: Boolean, description: String): Domain = Domain(name, enabled, description, Seq.empty)

  implicit class WithIdDomainExtensions[F[_]](domain: WithId[Domain])(implicit client: KeystoneClient[F], F: Sync[F])
    extends IdFetcher[Domain] with RoleAssigner[Domain] {

    /*private val idFetcher = new IdFetcher[Domain] {
      override def getWithId[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = F.pure(domain)
    }
    object roles {
      def users[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, Domain](idFetcher, client.domains.roles.users)
      def groups[F[_]: Sync](implicit client: KeystoneClient[F]) = new ContextualRoleAssignmentService[F, Domain](idFetcher, client.domains.roles.groups)
    }*/
    override def getWithId[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = F.pure(domain)
    override def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F] = client.domains
  }
}

case class Domain(
  name: String,
  enabled: Boolean,
  description: String,
  tags: Seq[String]
) extends Enabler[Domain] with IdFetcher[Domain] with RoleAssigner[Domain] {
  override def withEnabled(enabled: Boolean): Domain = copy(enabled = enabled)

  override def getWithId[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.getByName(name)

  override def service[F[_]](implicit client: KeystoneClient[F]): Domains[F] = client.domains
}

