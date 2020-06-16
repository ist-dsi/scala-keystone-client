package pt.tecnico.dsi.keystone.models.auth

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Project {
  implicit val codec: Codec.AsObject[Project] = deriveCodec(renaming.snakeCase)

  def apply(id: String): Project = new Project(Some(id), None, None)
  def apply(name: String, domain: Domain): Project = new Project(None, Some(name), Some(domain))

  def fromEnvironment(env: Map[String, String]): Option[Project] = {
    val idOpt = env.get("OS_PROJECT_ID").map(id => Project(id))
    val nameOpt = env.get("OS_PROJECT_NAME").zip(Domain.fromEnvironment(env, "OS_PROJECT"))
      .map { case (name, domain) => Project(name, domain) }
    idOpt orElse nameOpt
  }
}

case class Project private (id: Option[String], name: Option[String], domain: Option[Domain])