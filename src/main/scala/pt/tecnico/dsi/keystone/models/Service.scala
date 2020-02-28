package pt.tecnico.dsi.keystone.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.syntax._
import org.http4s.dsl.impl./

/*
sealed trait Type
object Type {
  object Compute extends Type
  object Ec2 extends Type
  object Identity extends Type
  object Image extends Type
  object Network extends Type
  object Volume extends Type

  implicit val encoder: Encoder[Type] = {
    case Compute => "compute".asJson
    case Ec2 => Json.fromString("ec2")
    case Identity => Json.fromString("identity")
    case Image => Json.fromString("image")
    case Network => Json.fromString("network")
    case Volume => Json.fromString("volume")
  }
  implicit val decoder: Decoder[Type] = Decoder.decodeString.map[Type] {
    case "compute" => Compute
    case "ec2" => Ec2
    case "identity" => Identity
    case "image" => Image
    case "network" => Network
    case "volume" => Volume
  }

/*  def fromString(name: String): Type = {
    case "compute" => Compute
    case "ec2" => Ec2
    case "identity" => Identity
    case "image" => Image
    case "network" => Network
    case "volume" => Volume
  }*/
}
*/


object Service {

  implicit val decoder: Decoder[Service] = deriveDecoder(renaming.snakeCase, true, None)

  implicit val encoder: Encoder[Service] = deriveEncoder(renaming.snakeCase, None)

    def apply(name: String, `type`: String, description: String, enabled: Boolean = true): Service =
      Service(name, `type`, Some(description), enabled)
}

// *links* is auto-generated by openstack
case class Service private[keystone] (
  name: String,
  `type`: String,
  description: Option[String],
  enabled: Boolean,
// links: Map[String, String] = Map.empty
)