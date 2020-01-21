package pt.tecnico.dsi.keystone.domains.models

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{deriveDecoder, renaming, deriveEncoder}

object DomainWrapper {
  implicit val decoder: Decoder[DomainWrapper] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[DomainWrapper] = deriveEncoder(renaming.snakeCase, None)
}

case class DomainWrapper(
  domain: Domain
)
