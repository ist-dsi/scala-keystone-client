package pt.tecnico.dsi.openstack.keystone.models

import cats.Show
import enumeratum.{Circe, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

sealed trait Interface extends EnumEntry
case object Interface extends Enum[Interface] {
  implicit val circeEncoder: Encoder[Interface] = Circe.encoderLowercase(this)
  implicit val circeDecoder: Decoder[Interface] = Circe.decoderLowercaseOnly(this)

  case object Public extends Interface
  case object Admin extends Interface
  case object Internal extends Interface

  val values = findValues
  
  implicit val show: Show[Interface] = Show.fromToString
}