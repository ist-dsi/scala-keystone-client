package pt.tecnico.dsi.keystone.models.auth

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Domain {
  implicit val codec: Codec.AsObject[Domain] = deriveCodec(renaming.snakeCase, false, None)

  // Unfortunately these methods cannot be named apply
  def id(id: String): Domain = Domain(Some(id), None)
  def name(name: String): Domain = Domain(None, Some(name))

  def apply(id: String, name: String): Domain = Domain(Some(id), Some(name))
}

case class Domain private (id: Option[String], name: Option[String])
