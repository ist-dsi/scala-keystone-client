package pt.tecnico.dsi.keystone.models.domains

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder, renaming}

object Domains {
  implicit val decoder: Decoder[Domains] = deriveDecoder(renaming.snakeCase, false, None)
}

case class Domains(domains: Seq[Domain], links: Links)