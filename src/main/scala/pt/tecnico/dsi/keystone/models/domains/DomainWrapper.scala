package pt.tecnico.dsi.keystone.models.domains

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object DomainWrapper {
  implicit val decoder: Decoder[DomainWrapper] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[DomainWrapper] = deriveEncoder(renaming.snakeCase, None)
}

case class DomainWrapper(
  domain: Domain
)
