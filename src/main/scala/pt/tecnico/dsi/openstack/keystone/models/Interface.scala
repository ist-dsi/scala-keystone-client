package pt.tecnico.dsi.openstack.keystone.models

import cats.Show
import enumeratum.EnumEntry.Lowercase
import enumeratum.{CirceEnum, Enum, EnumEntry}

sealed trait Interface extends EnumEntry with Lowercase
case object Interface extends Enum[Interface] with CirceEnum[Interface] {
  case object Public extends Interface
  case object Admin extends Interface
  case object Internal extends Interface
  
  val values = findValues
  
  implicit val show: Show[Interface] = Show.fromToString
}