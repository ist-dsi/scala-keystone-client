package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import pt.tecnico.dsi.openstack.keystone.models.Service

class ServiceSpec extends CrudSpec[Service]("service", _.services) with EnableDisableSpec[Service] {
  def stub = IO.pure(Service(
    name = "service",
    description = "service-desc",
    enabled = true,
    `type` = "service-type"
  ))
}
