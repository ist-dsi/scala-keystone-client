package pt.tecnico.dsi.keystone.domains.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}

object Domain {
  implicit val decoder: Decoder[Domain] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Domain] = deriveEncoder(renaming.snakeCase, None)
}

case class Domain(
  description: Option[String],
  enabled: Option[Boolean],
  id: Option[String],
  links: Option[Links],
  name: Option[String],
  tags: Option[Seq[String]]
)


