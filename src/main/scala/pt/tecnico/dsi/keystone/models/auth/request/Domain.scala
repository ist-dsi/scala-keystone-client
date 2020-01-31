package pt.tecnico.dsi.keystone.models.auth.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Domain {
	implicit val encoder: Encoder[Domain] = deriveEncoder(renaming.snakeCase, None)
}

case class Domain(
	name: Option[String],
	id: Option[String]
)
