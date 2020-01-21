package pt.tecnico.dsi.keystone.domains.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}
import pt.tecnico.dsi.keystone.auth.models.response.Token

object ListResponse {
  implicit val decoder: Decoder[ListResponse] = deriveDecoder(renaming.snakeCase, false, None)
}

case class ListResponse(token: Token)

