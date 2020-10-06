package pt.tecnico.dsi.openstack.keystone.models

import io.circe.Decoder
import io.circe.derivation.{deriveDecoder,renaming}

object KeystoneError {
  implicit val decoder: Decoder[KeystoneError] = deriveDecoder(renaming.snakeCase).at("error")
}
case class KeystoneError(message: String, code: Int, title: String) extends Exception(message)
