package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Role {
  implicit val codec: Codec.AsObject[Role] = deriveCodec(renaming.snakeCase)
}
case class Role(
  name: String,
  description: Option[String],
  domainId: Option[String],
)