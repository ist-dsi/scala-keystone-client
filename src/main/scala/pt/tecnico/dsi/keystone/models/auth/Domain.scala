package pt.tecnico.dsi.keystone.models.auth

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Domain {
  implicit val codec: Codec.AsObject[Domain] = deriveCodec(renaming.snakeCase, false, None)

  // Unfortunately these methods cannot be named apply
  def id(id: String): Domain = Domain(Some(id), None)
  def name(name: String): Domain = Domain(None, Some(name))

  def apply(id: String, name: String): Domain = Domain(Some(id), Some(name))

  def fromEnvironment(env: Map[String, String], prefix: String = "OS"): Option[Domain] = {
    val idOpt = env.get(s"${prefix}_DOMAIN_ID") orElse env.get("OS_DEFAULT_DOMAIN_ID") orElse env.get("OS_DEFAULT_DOMAIN")
    val nameOpt = env.get(s"${prefix}_DOMAIN_NAME") orElse env.get("OS_DEFAULT_DOMAIN_NAME")
    (idOpt, nameOpt) match {
      case (Some(id), Some(name)) => Some(Domain(id, name))
      case (Some(id), None) => Some(Domain.id(id))
      case (None, Some(name)) => Some(Domain.name(name))
      case _ => None
    }
  }
}

case class Domain private (id: Option[String], name: Option[String])