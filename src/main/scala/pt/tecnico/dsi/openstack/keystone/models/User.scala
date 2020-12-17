package pt.tecnico.dsi.openstack.keystone.models

import java.time.OffsetDateTime
import cats.derived
import cats.derived.ShowPretty
import cats.effect.Sync
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import io.chrisdavenport.cats.time.offsetdatetimeInstances
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient

object User {
  object Create {
    implicit val encoder: Encoder[Create] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Create] = derived.semiauto.showPretty
  }
  /**
   * Options to create a User
   *
   * @param name             The user name. Must be unique within the owning domain.
   * @param password         The password for the user.
   * @param domainId         The ID of the domain of the user. If the domain ID is not provided in the request, the Identity service will attempt to pull the
   *                         domain ID from the token used in the request. Note that this requires the use of a domain-scoped token.
   * @param defaultProjectId The ID of the default project for the user. A user’s default project must not be a domain.
   *                         Setting this attribute does not grant any actual authorization on the project, and is merely provided for convenience.
   *                         Therefore, the referenced project does not need to exist within the user domain.
   *                         If the user does not have authorization to their default project, the default project is ignored at token creation.
   *                         Additionally, if your default project is not valid, a token is issued without an explicit scope of authorization.
   * @param enabled          If the user is enabled, this value is true. If the user is disabled, this value is false.
   */
  case class Create(
    name: String,
    password: Option[String] = None,
    domainId: Option[String] = None,
    defaultProjectId: Option[String] = None,
    enabled: Boolean = true,
  )

  object Update {
    implicit val encoder: Encoder[Update] = deriveEncoder(renaming.snakeCase)
    implicit val show: ShowPretty[Update] = derived.semiauto.showPretty
  }
  /**
   * Options to update a User
   *
   * @param name The new name for the user. Must be unique within the owning domain.
   * @param password The new password for the user.
   * @param defaultProjectId The new ID of the default project for the user.
   * @param enabled Enables or disables the user. An enabled user can authenticate and receive authorization. A disabled user cannot authenticate
   *             or receive authorization. Additionally, all tokens that the user holds become no longer valid. If you reenable this user,
   *             pre-existing tokens do not become valid. To enable the user, set to true. To disable the user, set to false.
   */
  case class Update(
    name: Option[String] = None,
    password: Option[String] = None,
    defaultProjectId: Option[String] = None,
    enabled: Option[Boolean] = None,
  ) {
    lazy val needsUpdate: Boolean = {
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, password, defaultProjectId, enabled).exists(_.isDefined)
    }
  }
  
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
  
  implicit val show: ShowPretty[User] = derived.semiauto.showPretty
}
final case class User(
  id: String,
  name: String,
  domainId: String,
  defaultProjectId: Option[String] = None,
  passwordExpiresAt: Option[OffsetDateTime] = None,
  enabled: Boolean = true,
  links: List[Link] = List.empty,
) extends Identifiable { self =>
  def domain[F[_]](implicit client: KeystoneClient[F]): F[Domain] = client.domains(domainId)
  
  /** The groups to which the user belongs */
  def groups[F[_]: Sync](implicit client: KeystoneClient[F]): F[List[Group]] = client.users.listGroups(self.id)
  /** The projects to which the user belongs */
  def projects[F[_]: Sync](implicit client: KeystoneClient[F]): F[List[Project]] = client.users.listProjects(self.id)
  def changePassword[F[_]: Sync](originalPassword: String, password: String)(implicit client: KeystoneClient[F]): F[Unit] =
    client.users.changePassword(self.id, originalPassword, password)
  
  def addToGroup[F[_]: Sync](id: String)(implicit client: KeystoneClient[F]): F[Unit] = client.groups.addUser(id, self.id)
  def removeFromGroup[F[_]: Sync](id: String)(implicit client: KeystoneClient[F]): F[Unit] = client.groups.removeUser(id, self.id)
  def isInGroup[F[_]: Sync](id: String)(implicit client: KeystoneClient[F]): F[Boolean] = client.groups.isUserInGroup(id, self.id)
}