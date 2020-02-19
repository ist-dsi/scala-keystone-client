package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Domain {
  implicit val decoder: Decoder[Domain] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Domain] = deriveEncoder(renaming.snakeCase, None)

  def apply(name: String, enabled: Boolean, description: String): Domain = Domain(name, enabled, description, Seq.empty)
}

case class Domain(
  name: String,
  enabled: Boolean,
  description: String,
  tags: Seq[String]
)


