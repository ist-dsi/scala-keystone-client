package pt.tecnico.dsi.openstack.keystone.models

import io.circe.Decoder
import io.circe.derivation.ConfiguredDecoder

object KeystoneError:
  given Decoder[KeystoneError] = ConfiguredDecoder.derived[KeystoneError].at("error")
case class KeystoneError(message: String, code: Int, title: String) extends Exception(message)
