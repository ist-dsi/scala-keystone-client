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


  override def changeField[R](stub: Service): Service = stub.copy(name = stub.name + "-updated")

  override def withName(stub: Service)(newName: String => String): Service = stub.copy(name = newName(stub.name))
}
