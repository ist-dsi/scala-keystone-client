package pt.tecnico.dsi.keystone.models.groups

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Group {
  implicit val decoder: Decoder[Group] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Group] = deriveEncoder(renaming.snakeCase, None)
}

case class Group (
  description: String = null,
  links: Map[String, String] = Map.empty,
  domainId: String = null,
  name: String = null,
  id: String = null
)