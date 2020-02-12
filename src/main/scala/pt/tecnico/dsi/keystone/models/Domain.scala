package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Domain {
  implicit val decoder: Decoder[Domain] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Domain] = deriveEncoder(renaming.snakeCase, None)
}

case class Domain(
  description: String = null,
  enabled: Boolean = true,
  links: Map[String, String] = Map.empty,
  name: String = null,
  tags: Seq[String] = Seq.empty
)


