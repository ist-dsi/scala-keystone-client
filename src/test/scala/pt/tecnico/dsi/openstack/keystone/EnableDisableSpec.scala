package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.services.EnableDisableEndpoints

trait EnableDisableSpec[T <: Identifiable] { self: CrudSpec[T, _, _] =>
  lazy val enableDisableService = service.asInstanceOf[CrudService[IO, T, _, _] with EnableDisableEndpoints[IO, T]]

  def getEnabled(model: T): Boolean

  s"The ${name} service" should {
    s"enable a ${name}" in resource.use[IO, Assertion] { model =>
      enableDisableService.enable(model.id).idempotently(getEnabled(_) shouldBe true)
    }

    s"disable a ${name}" in resource.use[IO, Assertion] { model =>
      enableDisableService.disable(model.id).idempotently(getEnabled(_) shouldBe false)
    }
  }
}
