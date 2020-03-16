package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.keystone.KeystoneClient

object Group {
  implicit val codec: Codec.AsObject[Group] = deriveCodec(renaming.snakeCase, false, None)

  //def apply(name: String, description: String, domainId: String) = Group(name, description, domainId)
}

case class Group (
  name: String,
  description: String,
  domainId: String,
) {
  def domain[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.get(domainId)
}