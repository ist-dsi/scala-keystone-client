package pt.tecnico.dsi.keystone.models

import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}

object Domain {
  implicit val codec: Codec.AsObject[Domain] = deriveCodec(renaming.snakeCase, false, None)

  def apply(name: String, enabled: Boolean, description: String): Domain = Domain(name, enabled, description, Seq.empty)
}

case class Domain(
  name: String,
  enabled: Boolean,
  description: String,
  tags: Seq[String]
) extends WithEnabled[Domain] {
  override def withEnabled(enabled: Boolean): Domain = copy(enabled = enabled)
}

