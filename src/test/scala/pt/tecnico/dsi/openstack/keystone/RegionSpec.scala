package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import pt.tecnico.dsi.openstack.keystone.models.Region

class RegionSpec extends CrudSpec[Region]("region", _.regions) {
  def stub = IO.pure(Region(
    description = "region description"
  ))
}