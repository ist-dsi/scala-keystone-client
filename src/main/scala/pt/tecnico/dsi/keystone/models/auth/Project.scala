package pt.tecnico.dsi.keystone.models.auth

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Project {
  implicit val codec: Codec.AsObject[Project] = deriveCodec(renaming.snakeCase, false, None)

  def apply(id: String): Project = new Project(Some(id), None, None)
  def apply(name: String, domain: Domain): Project = new Project(None, Some(name), Some(domain))
}

case class Project private (id: Option[String], name: Option[String], domain: Option[Domain])