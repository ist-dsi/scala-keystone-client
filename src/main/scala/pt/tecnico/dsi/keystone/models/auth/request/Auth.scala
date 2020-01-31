package pt.tecnico.dsi.keystone.models.auth.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Auth {
	implicit val encoder: Encoder[Auth] = deriveEncoder(renaming.snakeCase, None)

	def apply(identity: Identity, scope: Scope): Auth = Auth(identity, Some(scope))
}

case class Auth(
	identity: Identity,
	scope: Option[Scope] = None
)


