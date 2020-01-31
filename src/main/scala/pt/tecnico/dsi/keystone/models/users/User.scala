package pt.tecnico.dsi.keystone.models.users

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import pt.tecnico.dsi.keystone.models.domains.Domain

object User {
  implicit val decoder: Decoder[User] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[User] = deriveEncoder(renaming.snakeCase, None)
}

case class User(
  id: String = null,
  name: String = null,
  email: String = null,
  description: String = null,
  password: String = null,
  defaultProjectId: String = null,
  domainId: String = null,
  domain: Domain = null,
  links: Map[String, String] = Map.empty,
  enabled: Boolean = true
)