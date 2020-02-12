package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Group {
  implicit val decoder: Decoder[Group] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Group] = deriveEncoder(renaming.snakeCase, None)
}

case class Group(
  description: String = null,
  links: Map[String, String] = Map.empty,
  domainId: String = null,
  name: String = null,
)