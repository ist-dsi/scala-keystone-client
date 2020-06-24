package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import io.circe.Encoder
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.keystone.models.{Group, User, WithId}
import org.http4s.Method.{HEAD, PUT}

final class Groups[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CrudService[F, Group](baseUri, "group", authToken)
  with UniqueWithinDomain[F, Group] {
  import dsl._

  /**
    * @param name filters the response by a group name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of groups filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): Stream[F, WithId[Group]] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
    )))


  override def create(group: Group)(implicit encoder: Encoder[Group]): F[WithId[Group]] = createHandleConflict(group) { _ =>
    get(group.name, group.domainId).flatMap(existingGroup => update(existingGroup.id, group))
  }

  //TODO: passwordExpiresAt should not be a string
  //https://docs.openstack.org/api-ref/identity/v3/index.html?expanded=check-whether-user-belongs-to-group-detail,list-users-in-group-detail#list-users-in-group
  /**
    * Lists the users that belong to a group.
    * @param id the group id.
    * @param passwordExpiresAt
    */
  def listUsers(id: String, passwordExpiresAt: Option[String] = None): Stream[F, WithId[User]] = {
    val query = passwordExpiresAt.fold(Query.empty)(value => Query.fromPairs("password_expires_at" -> value))
    super.list[WithId[User]]("users", uri / id / "users", query)
  }

  def addUser(id: String, userId: String): F[Unit] = client.expect(PUT(uri / id / "users" / userId))
  def removeUser(id: String, userId: String): F[Unit] = super.delete(uri / id / "users" / userId)
  def isUserInGroup(id: String, userId: String): F[Boolean] = client.successful(HEAD(uri / id / "users" / userId))
}
