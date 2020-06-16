package pt.tecnico.dsi.keystone.models.auth

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}

object Credential {
  implicit val encoder: Encoder.AsObject[Credential] = deriveEncoder(renaming.snakeCase)

  def apply(id: String, password: String): Credential = new Credential(Some(id), None, password, None)
  def apply(name: String, password: String, domain: Domain): Credential = new Credential(None, Some(name), password, Some(domain))

  def fromEnvironment(env: Map[String, String]): Option[Credential] =
    env.get("OS_PASSWORD").flatMap { password =>
      val idOpt = env.get("OS_USER_ID").map(id => Credential(id, password))
      val nameOpt = env.get("OS_USERNAME").zip(Domain.fromEnvironment(env, "OS_USER"))
        .map { case (name, domain) => Credential(name, password, domain) }

      idOpt orElse nameOpt
    }
}
case class Credential private (id: Option[String], name: Option[String], password: String, domain: Option[Domain])