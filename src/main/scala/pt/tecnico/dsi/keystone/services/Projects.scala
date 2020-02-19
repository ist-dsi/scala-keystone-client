package pt.tecnico.dsi.keystone.services

import org.http4s._
import cats.effect.Sync
import pt.tecnico.dsi.keystone.models.{Project, WithId}
import cats.syntax.functor._
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client

class Projects[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Project](baseUri, "project", subjectToken) {
  /**
    * TODO: get by name, and get by name and domain_id
    */

  def get(name: String, domainId: String): Stream[F, WithId[Project]] = list(Query.fromPairs(
    "name" -> name,
    "domain_id" -> domainId,
  ))

  override def create(project: Project): F[WithId[Project]] = createHandleConflict(project) {
    // The user name must be unique within the owning domain.
    // If we got a conflict then a user with this name must already exist.
    get(project.name, project.domainId).compile.lastOrError.flatMap { existingProject =>
      update(existingProject)
    }
  }
}