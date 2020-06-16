package pt.tecnico.dsi.keystone.models

import java.time.OffsetDateTime
import cats.effect.Sync
import fs2.Stream
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder}
import pt.tecnico.dsi.keystone.KeystoneClient

object User {
  /*The decoder must handle the user returned from GET /v3/users/${id} and the user return from the /v3/auth/tokens
   The user from the tokens does not have a field named domain_id. Instead it has:
   "domain": {
     "id": "default",
     "name": "Default"
   }
   We simply ignore the domain name and read domain.id directly to domainId. The domain can be easily obtained from
   the User domain class*/
  implicit val decoder: Decoder[User] = deriveDecoder(renaming.snakeCase).prepare { cursor =>
    val domainIdCursor = cursor.downField("domain").downField("id")
    domainIdCursor.as[String] match {
      case Right(domainId) => domainIdCursor.up.delete.withFocus(_.mapObject(_.add("domain_id", domainId.asJson)))
      case Left(_) => cursor
    }
  }
  implicit val encoder: Encoder.AsObject[User] = deriveEncoder(renaming.snakeCase).mapJsonObject(_.remove("password_expires_at"))
  implicit val codec: Codec[User] = Codec.from(decoder, encoder)

  def apply(name: String, domainId: String, defaultProjectId: Option[String], enabled: Boolean): User =
    new User(name, domainId, defaultProjectId, None, enabled)

  implicit class WithIdUserExtensions[F[_]](user: WithId[User])(implicit client: KeystoneClient[F], F: Sync[F]) {
    /** The groups to which the user belongs */
    val groups: Stream[F, Group] = client.users.listGroups(user.id)
    /** The projects to which the user belongs */
    val projects: Stream[F, Project] = client.users.listProjects(user.id)

    def changePassword(originalPassword: String, password: String): F[Unit] =
      client.users.changePassword(user.id, originalPassword, password)

    def addToGroup(id: String): F[Unit] = client.groups.addUser(id, user.id)
    def removeFromGroup(id: String): F[Unit] = client.groups.removeUser(id, user.id)
    def isInGroup(id: String): F[Boolean] = client.groups.isUserInGroup(id, user.id)
  }
}

case class User(
  name: String,
  domainId: String,
  defaultProjectId: Option[String] = None,
  passwordExpiresAt: Option[OffsetDateTime] = None,
  enabled: Boolean = true,
  // TODO: handle the extra attributes
) extends Enabler[User] with IdFetcher[User] {
  override def getWithId[F[_]: Sync](implicit client: KeystoneClient[F]): F[WithId[User]] = client.users.get(name, domainId)

  override def withEnabled(enabled: Boolean): User = copy(enabled = enabled)

  def domain[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.get(domainId)

  /** The groups to which the user belongs */
  def groups[F[_]: Sync: KeystoneClient]: Stream[F, Group] = withId(_.groups)
  /** The projects to which the user belongs */
  def projects[F[_]: Sync: KeystoneClient]: Stream[F, Project] = withId(_.projects)
  def changePassword[F[_]: Sync: KeystoneClient](originalPassword: String, password: String): F[Unit] =
    withId(_.changePassword(originalPassword, password))

  def addToGroup[F[_]: Sync: KeystoneClient](id: String): F[Unit] = withId(_.addToGroup(id))
  def removeFromGroup[F[_]: Sync: KeystoneClient](id: String): F[Unit] = withId(_.removeFromGroup(id))
  def isInGroup[F[_]: Sync: KeystoneClient](id: String): F[Boolean] = withId(_.isInGroup(id))
}