package pt.tecnico.dsi.keystone.models.groups

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object GroupWrapper {
  implicit val decoder: Decoder[GroupWrapper] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[GroupWrapper] = deriveEncoder(renaming.snakeCase, None)
}

case class GroupWrapper(group: Group)