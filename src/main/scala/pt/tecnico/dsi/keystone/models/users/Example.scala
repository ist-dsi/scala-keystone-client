package pt.tecnico.dsi.keystone.models.users

import io.circe.generic.extras.ConfiguredJsonCodec
import pt.tecnico.dsi.keystone.models.Entity.codecConfiguration

@ConfiguredJsonCodec
case class Example(coolTest: Int)