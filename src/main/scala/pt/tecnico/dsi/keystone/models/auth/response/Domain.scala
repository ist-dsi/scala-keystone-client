package pt.tecnico.dsi.keystone.models.auth.response

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Domain {
	implicit val decoder: Decoder[Domain] = deriveDecoder(renaming.snakeCase, false, None)
}

case class Domain (
	id: String,
	name: String
)
