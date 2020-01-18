package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Identity {
	implicit val encoder: Encoder[Identity] = deriveEncoder(renaming.snakeCase, None)
}

case class Identity (methods: Seq[String], password: Password)