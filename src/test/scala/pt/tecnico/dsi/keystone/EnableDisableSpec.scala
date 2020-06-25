package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.Enabler
import pt.tecnico.dsi.keystone.services.{CrudService, EnableDisableEndpoints}

trait EnableDisableSpec[T <: Enabler[T]] { self: CrudSpec[T] =>
  s"The ${name} service" should {
    s"enable a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        val enableDisableService = service.asInstanceOf[CrudService[IO, Enabler[T]] with EnableDisableEndpoints[IO, T]]
        for {
          _ <- enableDisableService.enable(createdStub.id).valueShouldIdempotentlyBe(())
          get <- enableDisableService.get(createdStub.id)
        } yield get.model.enabled shouldBe true
      }
    }

    s"disable a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        val enableDisableService = service.asInstanceOf[CrudService[IO, Enabler[T]] with EnableDisableEndpoints[IO, T]]
        for {
          _ <- enableDisableService.disable(createdStub.id).valueShouldIdempotentlyBe(())
          get <- enableDisableService.get(createdStub.id)
        } yield get.model.enabled shouldBe false
      }
    }
  }
}
