package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.functor._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import pt.tecnico.dsi.keystone.models.users.{User, UserWrapper, Users}

class UserService[F[_]: Sync](uri: Uri, token: Header)(implicit client: Client[F]) {

  private val dsl = new Http4sClientDsl[F] with Methods {}
  import dsl._

  /**
    * Gets detailed information about a specified user by id
    * @param userId the user id
    * @return the user
    */
  def get(userId: String): F[User] = {
    client.expect[UserWrapper](
      GET(uri / userId, token)
    ).map(_.user)
  }

  /**
    * Gets detailed information about users matching specified name across all domains
    *
    * @param userName the user name
    * @return the list of users matching the name across all domains
    */
  def getByName(userName: String): F[Seq[User]] = {
    client.expect[Users](
      GET(uri.withQueryParam("name", userName), token)
    ).map(_.users)
  }

  /**
    * Get detailed information about a user specified by username and domain id
    *
    * @param userName the user name
    * @param domainId the domain identifier
    * @return the user or null if not found
    */
  def get(userName: String, domainId: String): F[User] = {
    client.expect[UserWrapper](GET(uri
      .withQueryParam("domain_id", domainId)
      .withQueryParam("name", userName),
      token
    )).map(_.user)
  }

  /**
    * Delete a user by id
    *
    * @param userId the user id
    */
  def delete(userId: String): F[Unit] = client.expect(DELETE(uri / userId, token))

  /**
    * Updates the password for or enables or disables a specified user.
    *
    * @param user the user set to update
    * @return the updated user
    */
  def update(user: User): F[User] = {
    client.expect[UserWrapper](
      PATCH(UserWrapper(user), uri / user.id, token)
    ).map(_.user)
  }

  /**
    * Creates a new user
    *
    * @param user the group
    * @return the newly created user
    */
  def create(user: User): F[User] = {
    client.expect[UserWrapper](
      POST(UserWrapper(user), uri, token)
    ).map(_.user)
  }

  /**
    * @return list of users
    */
  def list(): F[Seq[User]] = {
    client.expect[Users](
      GET(uri, token)
    ).map(_.users)
  }

}
