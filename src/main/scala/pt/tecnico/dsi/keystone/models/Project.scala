package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.services.{Domains, Projects}

object Project {
  implicit val codec: Codec.AsObject[Project] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, description: String, domainId: String,
            isDomain: Boolean = false, enabled: Boolean = true): Project =
    Project(name, description, domainId, isDomain, enabled, List.empty)
}

case class Project private[keystone] (
  name: String,
  description: String,
  domainId: String,
  isDomain: Boolean,
  enabled: Boolean,
  tags: List[String]
) extends WithEnabled[Project] with WithRoleAssignment[Project] {
  override def service[F[_]](implicit client: KeystoneClient[F]): Projects[F] = client.projects
  override def withId[F[_]](implicit client: KeystoneClient[F]): F[WithId[Project]] = service.get(name, domainId)
  override def withEnabled(enabled: Boolean): Project = copy(enabled = enabled)
}