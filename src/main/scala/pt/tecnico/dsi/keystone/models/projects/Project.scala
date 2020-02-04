package pt.tecnico.dsi.keystone.models.projects

import pt.tecnico.dsi.keystone.codecConfiguration
import io.circe.generic.extras.ConfiguredJsonCodec
import pt.tecnico.dsi.keystone.models.domains.Domain

@ConfiguredJsonCodec
case class Project(
  id: String,
  name: String,
  domain: Domain,
  domainId: String,
  description: String,
  links: Map[String, String] = Map.empty,
  parentId: String,
  subtree: String,
  parents: String,
  enabled: Boolean = true,
  extra: Map[String, String] = Map.empty,
  tags: Seq[String] = Seq.empty
)