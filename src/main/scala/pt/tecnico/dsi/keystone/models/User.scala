package pt.tecnico.dsi.keystone.models

import java.time.OffsetDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import pt.tecnico.dsi.keystone.KeystoneClient

object User {
  //TODO: the decoder must handle the user returned from GET /v3/users/${id} and the user return from the /v3/auth/tokens
  // The user from the tokens does not have a field named domain_id. Instead it has:
  // "domain": {
  //   "id": "default",
  //   "name": "Default"
  // }
  // We simply ignore the domain name and read domain.id directly to domainId. The domain can be easily obtained from
  // the User domain class
  implicit val decoder: Decoder[User] = deriveDecoder(renaming.snakeCase, true, None).prepare { cursor =>
    val domainIdCursor = cursor.downField("domain").downField("id")
    domainIdCursor.as[String] match {
      case Right(domainId) => domainIdCursor.up.delete.withFocus(_.mapObject(_.add("domain_id", domainId.asJson)))
      case Left(_) => cursor
    }
  }
  implicit val encoder: Encoder[User] = deriveEncoder(renaming.snakeCase, None).mapJsonObject(_.remove("password_expires_at"))

  def apply(name: String, domainId: String, defaultProjectId: Option[String], enabled: Boolean): User =
    new User(name, domainId, defaultProjectId, None, enabled)

  /*private def apply(name: String, domainId: String, defaultProjectId: Option[String], passwordExpiresAt: Option[OffsetDateTime], enabled: Boolean): User =
    new User(name, domainId, defaultProjectId, passwordExpiresAt, enabled)*/
}

case class User private[keystone] (
  name: String,
  domainId: String,
  defaultProjectId: Option[String] = None,
  passwordExpiresAt: Option[OffsetDateTime] = None,
  enabled: Boolean = true,
  // TODO: handle the extra attributes
) {
  def domain[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.get(domainId)
}