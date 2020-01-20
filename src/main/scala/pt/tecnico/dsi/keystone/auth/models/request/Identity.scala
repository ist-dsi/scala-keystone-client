package pt.tecnico.dsi.keystone.auth.models.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Identity {
	implicit val encoder: Encoder[Identity] = deriveEncoder(renaming.snakeCase, None)

	def apply(password: Password): Identity = Identity(Seq("password"), password = Some(password))
	def apply(token: Token): Identity = Identity(Seq("Token"), token = Some(token))
}

case class Identity(methods: Seq[String], password: Option[Password] = None, token: Option[Token] = None)

