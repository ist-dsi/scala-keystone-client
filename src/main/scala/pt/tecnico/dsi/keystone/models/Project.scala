package pt.tecnico.dsi.keystone.models

import cats.effect.Sync
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.keystone.KeystoneClient
import pt.tecnico.dsi.keystone.services.{Domains, Projects, RoleAssignment}

object Project {
  implicit val codec: Codec.AsObject[Project] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, description: String, domainId: String,
            isDomain: Boolean = false, enabled: Boolean = true): Project =
    Project(name, description, domainId, isDomain, enabled, List.empty)

  implicit class WithIdProjectExtensions[H[_]](project: WithId[Project])(implicit client: KeystoneClient[H], H: Sync[H])
    extends IdFetcher[Project] with RoleAssigner[Project] {

    override def getWithId[F[_]: Sync](implicit client: KeystoneClient[F]): F[WithId[Project]] = implicitly(Sync[F]).pure(project)
    override def service[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F] = project.service
  }
}

case class Project private[keystone] (
  name: String,
  description: String,
  domainId: String,
  isDomain: Boolean,
  enabled: Boolean,
  tags: List[String]
) extends Enabler[Project] with IdFetcher[Project] with RoleAssigner[Project] {
  override def withEnabled(enabled: Boolean): Project = copy(enabled = enabled)

  override def getWithId[F[_]: Sync](implicit client: KeystoneClient[F]): F[WithId[Project]] = client.projects.get(name, domainId)

  override def service[F[_]](implicit client: KeystoneClient[F]): Projects[F] = client.projects
}