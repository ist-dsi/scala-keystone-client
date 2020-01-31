package pt.tecnico.dsi.keystone.models

import io.circe.generic.extras.Configuration

object Entity {
  implicit val codecConfiguration: Configuration =
    Configuration.default.withSnakeCaseMemberNames
}
