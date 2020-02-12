package pt.tecnico.dsi.keystone.models.auth

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Domain {
	implicit val decoder: Decoder[Domain] = deriveDecoder(renaming.snakeCase, false, None)
	implicit val encoder: Encoder[Domain] = deriveEncoder(renaming.snakeCase, None)

	// Unfortunately these methods cannot be named apply
	def id(id: String): Domain = new Domain(Some(id), None)
	def name(name: String): Domain = new Domain(None, Some(name))

	def apply(id: String, name: String): Domain = new Domain(Some(id), Some(name))
}

case class Domain private (id: Option[String], name: Option[String])
