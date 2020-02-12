package pt.tecnico.dsi.keystone.services

import org.http4s._
import cats.effect.Sync
import pt.tecnico.dsi.keystone.models.{User, WithId}
import cats.syntax.functor._
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client

class Users[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, User](baseUri, "user", subjectToken) {

  def getByName(name: String): Stream[F, WithId[User]] = list(Query.fromPairs("name" -> name))

  /**
    * Get detailed information about a user specified by name and domain id
    *
    * @param name the user name
    * @param domainId the domain id
    * @return the stream of users matching the name in a specific domain
    */
  def get(name: String, domainId: String): Stream[F, WithId[User]] = list(Query.fromPairs(
    "name" -> name,
    "domain_id" -> domainId,
  ))

  /**
    * Lists groups for a specified user
    *
    * @param userId the user id
    * @return list of groups for a user
    */
  /*def listUserGroups(userId: String): F[Seq[Group]] = {
    client.expect[Groups](
      GET(uri / userId / "groups")
    ).map(_.groups)
  }*/

  /**
    * @param userId the user id
    * @return the domain of the user
    */
  /*def getUserDomain(userId: String): F[Domain] = {
    get(userId).flatMap(user => domains.get(user.domainId))
  }*/

  /**
    * @param userId           the user identifier
    * @param originalPassword the original password
    * @param password         the new password
    */
  /*def changePassword(userId: String, originalPassword: String, password: String): F[Unit] = {
    client.expect(POST(
      ChangePassword(originalPassword, password),
      uri / userId / password,
      token
    ))
  }
  */

  override def create(user: User): F[WithId[User]] = createHandleConflict(user) {
    // The user name must be unique within the owning domain.
    // If we got a conflict then a user with this name must already exist.
    get(user.name, user.domainId).compile.lastOrError.flatMap { existingUser =>
      update(existingUser.id, user)
    }
  }
}