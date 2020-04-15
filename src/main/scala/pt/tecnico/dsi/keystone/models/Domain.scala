package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.services.Domains

object Domain {
  implicit val codec: Codec.AsObject[Domain] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, enabled: Boolean, description: String): Domain = Domain(name, enabled, description, Seq.empty)
}

case class Domain(
  name: String,
  enabled: Boolean,
  description: String,
  tags: Seq[String]
) extends WithEnabled[Domain] with WithRoleAssignment[Domain] {
  override def withEnabled(enabled: Boolean): Domain = copy(enabled = enabled)
  override def service[F[_]](implicit client: KeystoneClient[F]): Domains[F] = client.domains
  override def withId[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = service.getByName(name)
}

