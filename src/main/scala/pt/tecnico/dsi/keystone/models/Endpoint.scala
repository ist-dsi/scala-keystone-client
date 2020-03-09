package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import io.circe.{Decoder, Encoder}
import org.http4s.Uri
import org.http4s.circe.{decodeUri, encodeUri}

sealed trait Interface
object Interface {
  case object Public extends Interface
  case object Admin extends Interface
  case object Internal extends Interface

  private implicit val config: Configuration = Configuration.default.copy(transformConstructorNames = _.toLowerCase)
  implicit val decoder: Decoder[Interface] = deriveEnumerationDecoder[Interface]
  implicit val encoder: Encoder[Interface] = deriveEnumerationEncoder[Interface]
}

object Endpoint {
  implicit val decoder: Decoder[Endpoint] = deriveDecoder(renaming.snakeCase, true, None)
  implicit val encoder: Encoder[Endpoint] = deriveEncoder(renaming.snakeCase, None)
}
case class Endpoint(
  interface: Interface,
  regionId: String,
  url: String, // Cannot be Uri because some urls contain interpolations, eg: "http://0.0.0.0:6007/v1/AUTH_%(tenant_id)s"
  serviceId: String,
  enabled: Boolean = true,
)
