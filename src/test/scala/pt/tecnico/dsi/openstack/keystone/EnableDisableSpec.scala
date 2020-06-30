package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import pt.tecnico.dsi.openstack.keystone.models.Enabler
import pt.tecnico.dsi.openstack.keystone.services.{CrudService, EnableDisableEndpoints}

trait EnableDisableSpec[T <: Enabler[T]] { self: CrudSpec[T] =>
  s"The ${name} service" should {
    s"enable a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        val enableDisableService = service.asInstanceOf[CrudService[IO, Enabler[T]] with EnableDisableEndpoints[IO, T]]
        for {
          _ <- enableDisableService.enable(createdStub.id).idempotently(_ shouldBe ())
          get <- enableDisableService.get(createdStub.id)
        } yield get.model.enabled shouldBe true
      }
    }

    s"disable a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        val enableDisableService = service.asInstanceOf[CrudService[IO, Enabler[T]] with EnableDisableEndpoints[IO, T]]
        for {
          _ <- enableDisableService.disable(createdStub.id).idempotently(_ shouldBe ())
          get <- enableDisableService.get(createdStub.id)
        } yield get.model.enabled shouldBe false
      }
    }
  }
}
