package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.Service

class ServiceSpec extends CRUDSpec[Service]("service", _.services, idempotent = false) {
  def stub = IO.pure(Service(
    name = "service",
    description = "service-desc",
    enabled = true,
    `type` = "service-type"
  ))
}
