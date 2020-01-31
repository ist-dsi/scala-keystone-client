package pt.tecnico.dsi.keystone.models.users

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Users {
  implicit val decoder: Decoder[Users] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Users] = deriveEncoder(renaming.snakeCase, None)
}

case class Users(users: Seq[User])