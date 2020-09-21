package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Method.{HEAD, PUT}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Group, Session, User}

final class Groups[F[_]: Sync: Client](baseUri: Uri, session: Session, authToken: Header)
  extends CrudService[F, Group, Group.Create, Group.Update](baseUri, "group", authToken)
  with UniqueWithinDomain[F, Group] {
  import dsl._

  /**
    * @param name filters the response by a group name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of groups filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): Stream[F, Group] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
    )))

  override def create(create: Group.Create, extraHeaders: Header*): F[Group] = createHandleConflict(create, extraHeaders:_*) {
    val domainId = create.domainId.getOrElse(session.scopedDomainId())
    // We got a Conflict so we must be able to find the existing Group
    apply(create.name, domainId).flatMap { existingGroup =>
      // Description is the only field that can be different
      if (existingGroup.description != create.description) {
        update(existingGroup.id, Group.Update(description = create.description), extraHeaders:_*)
      } else {
        Sync[F].pure(existingGroup)
      }
    }
  }

  //TODO: passwordExpiresAt should not be a string
  //https://docs.openstack.org/api-ref/identity/v3/index.html?expanded=check-whether-user-belongs-to-group-detail,list-users-in-group-detail#list-users-in-group
  /**
    * Lists the users that belong to a group.
    * @param id the group id.
    * @param passwordExpiresAt
    */
  def listUsers(id: String, passwordExpiresAt: Option[String] = None): Stream[F, User] = {
    val query = passwordExpiresAt.fold(Query.empty)(value => Query.fromPairs("password_expires_at" -> value))
    super.list[User]("users", uri / id / "users", query)
  }

  def addUser(id: String, userId: String): F[Unit] = client.expect(PUT(uri / id / "users" / userId))
  def removeUser(id: String, userId: String): F[Unit] = super.delete(uri / id / "users" / userId)
  def isUserInGroup(id: String, userId: String): F[Boolean] = client.successful(HEAD(uri / id / "users" / userId))
}
