package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Project {
  implicit val codec: Codec.AsObject[Project] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, description: String, domainId: String,
            isDomain: Boolean = false, enabled: Boolean = true): Project =
    Project(name, description, domainId, isDomain, enabled, List.empty)
}

case class Project private[keystone] (
  name: String,
  description: String,
  domainId: String,
  isDomain: Boolean,
  enabled: Boolean,
  tags: List[String]
) extends WithEnabled[Project] {
  override def withEnabled(enabled: Boolean): Project = copy(enabled = enabled)
}