package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Project {
  implicit val decoder: Decoder[Project] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Project] = deriveEncoder(renaming.snakeCase, None)

  def apply(name: String, description: String, domainId: String,
            isDomain: Boolean = false, enabled: Boolean = true): Project =
    Project(name, description, domainId, isDomain, enabled, List.empty)
}

case class Project private[keystone] (
  name: String,
  description: String,
  domainId: String,
  isDomain: Boolean,
  enabled: Boolean,
  tags: List[String]
)