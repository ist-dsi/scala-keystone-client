package pt.tecnico.dsi.keystone.models.groups

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Groups {
  implicit val decoder: Decoder[Groups] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Groups] = deriveEncoder(renaming.snakeCase, None)
}

case class Groups(groups: Seq[Group])