package pt.tecnico.dsi.keystone.models.auth

import io.circe.derivation.{deriveDecoder, deriveEncoder, renaming}
import io.circe.{Decoder, Encoder}

object Project {
  implicit val decoder: Decoder[Project] = deriveDecoder(renaming.snakeCase, false, None)
  implicit val encoder: Encoder[Project] = deriveEncoder(renaming.snakeCase, None)

  def apply(id: String): Project = new Project(Some(id), None, None)
  def apply(name: String, domain: Domain): Project = new Project(None, Some(name), Some(domain))
}

case class Project private (id: Option[String], name: Option[String], domain: Option[Domain])