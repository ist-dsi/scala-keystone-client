package pt.tecnico.dsi.openstack.keystone.models

import cats.derived.ShowPretty
import cats.{Show, derived}
import enumeratum.{Enum, EnumEntry}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.derivation.{deriveDecoder, deriveEncoder}

sealed trait Scope extends EnumEntry
case object Scope extends Enum[Scope] {
  object Project {
    def apply(id: String): Project = new Project(id, null, null)
    def apply(name: String, domain: Domain): Project = new Project(null, name, domain)
    
    def fromEnvironment(env: Map[String, String]): Option[Project] = {
      val idOpt = env.get("OS_PROJECT_ID").map(id => Project(id))
      val nameOpt = env.get("OS_PROJECT_NAME").zip(Domain.fromEnvironment(env, "OS_PROJECT"))
        .map { case (name, domain) => Project(name, domain) }
      idOpt orElse nameOpt
    }
    
    implicit val encoder: Encoder[Project] = (project: Project) => Json.obj(
      "id" -> Option(project.id).asJson,
      "name" -> Option(project.name).asJson,
      "domain" -> Option(project.domain).asJson
    ).dropNullValues
    implicit val decoder: Decoder[Project] = (cursor: HCursor) => for {
      id <- cursor.get[Option[String]]("id")
      name <- cursor.get[Option[String]]("name")
      domain <- cursor.get[Option[Domain]]("domain")
    } yield Project(id.orNull, name.orNull, domain.orNull)
    implicit val show: ShowPretty[Project] = derived.semiauto.showPretty
  }
  case class Project(id: String, name: String, domain: Domain) extends Scope
  
  object Domain {
    def id(id: String): Domain = Domain(id, null)
    def name(name: String): Domain = Domain(null, name)
    
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
    
    implicit val encoder: Encoder[Domain] = (domain: Domain) => Json.obj(
      "id" -> Option(domain.id).asJson,
      "name" -> Option(domain.name).asJson
    ).dropNullValues
    implicit val decoder: Decoder[Domain] = (cursor: HCursor) => for {
      id <- cursor.get[Option[String]]("id")
      name<- cursor.get[Option[String]]("name")
    } yield Domain(id.orNull, name.orNull)
    implicit val show: ShowPretty[Domain] = derived.semiauto.showPretty
  }
  case class Domain(id: String, name: String) extends Scope
  
  object System {
    implicit val encoder: Encoder[System] = deriveEncoder[System]
    implicit val decoder: Decoder[System] = deriveDecoder[System]
    implicit val show: Show[System] = derived.semiauto.show
  }
  case class System(all: Boolean = true) extends Scope
  
  case object Unscoped extends Scope {
    implicit val encoderUnscoped: Encoder[Unscoped.type] = (_: Unscoped.type) => "unscoped".asJson
    // Unscoped represents the absence of a scope, so its impossible to implement a decoder for it
    implicit val showUnscoped: Show[Unscoped.type] = Show.fromToString
  }
  
  def fromEnvironment(env: Map[String, String]): Option[Scope] = {
    val projectOpt = Project.fromEnvironment(env)
    val domainOpt = Domain.fromEnvironment(env)
    val systemOpt = env.get("OS_SYSTEM_SCOPE").map(value => System(value.toLowerCase == "all"))
    projectOpt orElse domainOpt orElse systemOpt
  }
  
  implicit val decoder: Decoder[Scope] = { cursor: HCursor =>
    // If we cannot decode to a Project|Domain|System Scope then it is the Unscoped by definition
    Project.decoder.at("project")(cursor)
           .orElse(Domain.decoder.at("domain")(cursor))
           .orElse(System.decoder.at("system")(cursor))
           .orElse(Right(Unscoped))
  }
  implicit val encoder: Encoder[Scope] = {
    case project: Project => Json.obj("project" -> project.asJson)
    case domain: Domain => Json.obj("domain" -> domain.asJson)
    case system: System => Json.obj("system" -> system.asJson)
    case unscoped: Unscoped.type => unscoped.asJson
  }
  implicit val showUnscoped: Show[Scope] = derived.semiauto.showPretty
  
  val values = findValues
}