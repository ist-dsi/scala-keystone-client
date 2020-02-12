package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.{Domain, WithId}

class Domains[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Domain](baseUri, "domain", subjectToken) {

  def getByName(name: String): Stream[F, WithId[Domain]] = list(Query.fromPairs("name" -> name))
}
