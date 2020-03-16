package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.{Group, WithId}

class Groups[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Group](baseUri, "group", subjectToken) {

  def getByName(name: String): Stream[F, WithId[Group]] = list(Query.fromPairs("name" -> name))

  /**
    * Get detailed information about groups matching specified by name and domain
    *
    * @param name the group name
    * @param domainId  the domain id
    * @return the list of groups matching the name in a specific domain or null if not found
    */
  def get(name: String, domainId: String): Stream[F, WithId[Group]] = list(Query.fromPairs(
    "name" -> name,
    "domain_id" -> domainId,
  ))

  // Make group create idempotent
  override def create(group: Group): F[WithId[Group]] = createHandleConflict(group) {
    get(group.name, group.domainId).compile.lastOrError.flatMap(update)
  }
}
