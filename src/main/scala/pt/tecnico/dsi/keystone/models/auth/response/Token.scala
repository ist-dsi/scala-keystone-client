package pt.tecnico.dsi.keystone.models.auth.response

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Token {
	implicit val decoder: Decoder[Token] = deriveDecoder(renaming.snakeCase, false, None)
}

case class Token (
	methods: Seq[String],
	expiresAt: String,
	user: User,
	auditIds: Seq[String],
	issuedAt: String
)

