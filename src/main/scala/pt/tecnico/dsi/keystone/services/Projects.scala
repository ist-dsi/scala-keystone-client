package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.keystone.models.{Project, WithId}

class Projects[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Project](baseUri, "project", authToken) with UniqueWithinDomain[F, Project] {

  /**
    * @param name filters the response by a project name.
    * @param domainId filters the response by a domain ID.
    * @param enabled filters the response by either enabled (true) or disabled (false) projects.
    * @param isDomain if this is specified as true, then only projects acting as a domain are included. Otherwise, only projects that are not acting as a domain are included.
    * @param parentId filters the response by a parent ID
    * @return a stream of project filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None, enabled: Option[Boolean],
           isDomain: Option[Boolean] = None, parentId: Option[String] = None): Stream[F, WithId[Project]] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
      "enabled" -> enabled.map(_.toString),
      "is_domain" -> isDomain.map(_.toString),
      "parent_id" -> parentId,
    )))

  override def create(project: Project): F[WithId[Project]] = createHandleConflict(project) { _ =>
    get(project.name, project.domainId).flatMap(existingProject => update(existingProject.id, project))
  }
}