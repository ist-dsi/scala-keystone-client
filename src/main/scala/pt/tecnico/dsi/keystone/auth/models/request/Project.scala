package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Project {
  implicit val encoder: Encoder[System] = deriveEncoder(renaming.snakeCase, None)
}

case class Project(
  id: Option[String],
  domain: Option[Domain],
  name: Option[String]
)