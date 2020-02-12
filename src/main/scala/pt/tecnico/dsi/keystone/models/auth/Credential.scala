package pt.tecnico.dsi.keystone.models.auth

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Credential {
  implicit val encoder: Encoder[Credential] = deriveEncoder[Credential](renaming.snakeCase, None)

  def apply(id: String, password: String): Credential = new Credential(Some(id), None, password, None)
  def apply(name: String, password: String, domain: Domain): Credential = new Credential(None, Some(name), password, Some(domain))
}

case class Credential private (id: Option[String], name: Option[String], password: String, domain: Option[Domain])