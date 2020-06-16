package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Service {
  implicit val codec: Codec.AsObject[Service] = deriveCodec(renaming.snakeCase)

  def apply(name: String, `type`: String, description: String, enabled: Boolean = true): Service =
    Service(name, `type`, Some(description), enabled)
}

case class Service private[keystone] (
  name: String,
  `type`: String,
  description: Option[String],
  enabled: Boolean,
) extends Enabler[Service] {
  override def withEnabled(enabled: Boolean): Service = copy(enabled = enabled)
}