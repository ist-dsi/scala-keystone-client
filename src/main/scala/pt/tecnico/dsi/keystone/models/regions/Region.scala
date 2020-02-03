package pt.tecnico.dsi.keystone.models.regions

import pt.tecnico.dsi.keystone.codecConfiguration
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
case class RegionWrapper(region: Region)

@ConfiguredJsonCodec
case class Region(
   id: String,
   description: String,
   parentRegionId: String
)

@ConfiguredJsonCodec
case class Regions(regions: Seq[Region])