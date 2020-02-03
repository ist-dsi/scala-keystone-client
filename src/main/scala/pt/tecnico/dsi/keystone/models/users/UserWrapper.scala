package pt.tecnico.dsi.keystone.models.users

import pt.tecnico.dsi.keystone.codecConfiguration
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class UserWrapper(user: User)