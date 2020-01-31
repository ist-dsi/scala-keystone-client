package pt.tecnico.dsi.keystone.models.auth.request

import io.circe.Encoder
import io.circe.derivation.{deriveEncoder, renaming}
import pt.tecnico.dsi.keystone.models.auth.request

object Scope {
  implicit val encoder: Encoder[Scope] = deriveEncoder(renaming.snakeCase, None)
}
case class Scope(
                  system: Option[request.System],
                  domain: Option[Domain],
                  project: Option[Project]
)


