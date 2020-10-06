package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import org.http4s.Method.{HEAD, PUT}
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Group, KeystoneError, Session, User}

final class Groups[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Group, Group.Create, Group.Update](baseUri, "group", session.authToken)
  with UniqueWithinDomain[F, Group] {
  import dsl._

  /**
    * @param name filters the response by a group name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of groups filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): F[List[Group]] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
    )))
  
  override def update(id: String, update: Group.Update, extraHeaders: Header*): F[Group] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override def defaultResolveConflict(existing: Group, create: Group.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[Group] = {
    val updated = Group.Update(
      description = Option(create.description).filter(_ != existing.description),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def createOrUpdate(create: Group.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (Group, Group.Create) => F[Group] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Group] = {
    val conflicting = """.*?Duplicate entry found with name ([^ ]+) at domain ID ([^.]+)\.""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders) {
      case KeystoneError(conflicting(name, domainId), Conflict.code, _) => apply(name, domainId).flatMap(resolveConflict(_, create))
    }
  }
  
  //TODO: passwordExpiresAt should not be a string
  //https://docs.openstack.org/api-ref/identity/v3/index.html?expanded=check-whether-user-belongs-to-group-detail,list-users-in-group-detail#list-users-in-group
  /**
    * Lists the users that belong to a group.
    * @param id the group id.
    * @param passwordExpiresAt
    */
  def listUsers(id: String, passwordExpiresAt: Option[String] = None): F[List[User]] = {
    val query = passwordExpiresAt.fold(Query.empty)(value => Query.fromPairs("password_expires_at" -> value))
    super.list[User]("users", uri / id / "users", query)
  }

  def addUser(id: String, userId: String): F[Unit] = client.expect(PUT(uri / id / "users" / userId))
  def removeUser(id: String, userId: String): F[Unit] = super.delete(uri / id / "users" / userId)
  def isUserInGroup(id: String, userId: String): F[Boolean] = client.successful(HEAD(uri / id / "users" / userId))
}
