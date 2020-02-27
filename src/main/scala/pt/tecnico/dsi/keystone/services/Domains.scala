package pt.tecnico.dsi.keystone.services

import org.http4s._
import cats.effect.Sync
import pt.tecnico.dsi.keystone.models.{Domain, WithId}
import cats.syntax.functor._
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client

class Domains[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Domain](baseUri, "domain", subjectToken) {

  def getByName(name: String): Stream[F, WithId[Domain]] = list(Query.fromPairs("name" -> name))

  override def create(domain: Domain): F[WithId[Domain]] = createHandleConflict(domain) {
    // The user name must be unique within the owning domain.
    // If we got a conflict then a user with this name must already exist.
    getByName(domain.name).compile.lastOrError.flatMap { existingDomain =>
      update(existingDomain)
    }
  }

  override def delete(id: String): F[Unit] = {
    for {
      // We need to disable the domain before deleting it
      // see https://docs.openstack.org/api-ref/identity/v3/?expanded=delete-domain-detail
      domain <- get(id)
      _ <- update(domain.id, domain.model.copy(enabled = false))
    } yield super.delete(id)
  }

}
