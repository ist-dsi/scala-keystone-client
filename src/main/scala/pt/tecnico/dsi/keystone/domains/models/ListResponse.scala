package pt.tecnico.dsi.keystone.domains.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object ListResponse {
  implicit val decoder: Decoder[ListResponse] = deriveDecoder(renaming.snakeCase, false, None)
}

case class ListResponse(domains: Seq[Domain], links: Links)