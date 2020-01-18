package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Domain {
	implicit val encoder: Encoder[Domain] = deriveEncoder(renaming.snakeCase, None)
}

case class Domain(name: String)
