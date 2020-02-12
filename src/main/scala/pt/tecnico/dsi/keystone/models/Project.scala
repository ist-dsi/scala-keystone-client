package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Project {
  implicit val decoder: Decoder[Project] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Project] = deriveEncoder(renaming.snakeCase, None)
}

case class Project(
  name: String,
  description: String,
  domainId: String,
  parentId: Option[String] = None,
  isDomain: Boolean = false,
  enabled: Boolean = true,
  tags: List[String] = List.empty
)