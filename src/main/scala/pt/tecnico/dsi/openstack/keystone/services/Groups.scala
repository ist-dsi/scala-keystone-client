package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Concurrent
import cats.syntax.flatMap._
import org.http4s.Method.{HEAD, PUT}
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Group, KeystoneError, Session, User}

/**
 * The service class for groups.
 * @define domainModel group
 */
final class Groups[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Group, Group.Create, Group.Update](baseUri, "group", session.authToken)
    with UniqueWithinDomain[F, Group] {
  
  /**
    * @param name filters the response by a group name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of groups filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): F[List[Group]] =
    list(Query("name" -> name, "domain_id" -> domainId))

  override def defaultResolveConflict(existing: Group, create: Group.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[Group] = {
    val updated = Group.Update(
      description = Option(create.description).filter(_ != existing.description),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Concurrent[F].pure(existing)
  }
  override def createOrUpdate(create: Group.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (Group, Group.Create) => F[Group] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Group] = {
    val conflicting = """.*?Duplicate entry found with name ([^ ]+) at domain ID ([^.]+)\.""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders) {
      case KeystoneError(conflicting(name, domainId), Conflict.code, _) =>
        apply(name, domainId).flatMap { existing =>
          getLogger.info(s"createOrUpdate: found unique ${this.name} (id: ${existing.id}) with the correct name, on domain with id $domainId.")
          resolveConflict(existing, create)
        }
    }
  }
  
  override def update(id: String, update: Group.Update, extraHeaders: Header.ToRaw*): F[Group] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  //TODO: passwordExpiresAt should not be a string
  //https://docs.openstack.org/api-ref/identity/v3/index.html?expanded=check-whether-user-belongs-to-group-detail,list-users-in-group-detail#list-users-in-group
  /**
    * Lists the users that belong to a group.
    * @param id the group id.
    * @param passwordExpiresAt
    */
  def listUsers(id: String, passwordExpiresAt: Option[String] = None): F[List[User]] = {
    val query = passwordExpiresAt.fold(Query.empty)(value => Query.fromPairs("password_expires_at" -> value))
    super.list[User]("users", (uri / id / "users").copy(query = query))
  }
  
  def addUser(id: String, userId: String): F[Unit] = super.expect(wrappedAt = None, PUT, uri / id / "users" / userId)
  def removeUser(id: String, userId: String): F[Unit] = super.delete(uri / id / "users" / userId)
  def isUserInGroup(id: String, userId: String): F[Boolean] = {
    import dsl._
    client.successful(HEAD(uri / id / "users" / userId, authToken))
  }
}
