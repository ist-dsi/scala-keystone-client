package pt.tecnico.dsi.keystone.models

import java.time.OffsetDateTime

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}
import pt.tecnico.dsi.keystone.KeystoneClient

object Group {
  implicit val decoder: Decoder[Group] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Group] = deriveEncoder(renaming.snakeCase, None)

  //def apply(name: String, description: String, domainId: String) = Group(name, description, domainId)
}

case class Group (
  name: String,
  description: String,
  domainId: String,
) {
  def domain[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.get(domainId)
}