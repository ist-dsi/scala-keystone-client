package pt.tecnico.dsi.keystone

import cats.effect.IO
import io.circe.Codec
import org.scalatest.Assertion
import pt.tecnico.dsi.keystone.models.{Enabler, WithId}
import pt.tecnico.dsi.keystone.services.{CrudService, EnableDisableEndpoints}

abstract class CrudSpec[T](val name: String, val service: KeystoneClient[IO] => CrudService[IO, T], idempotent: Boolean = true)
                          (implicit val codec: Codec[T]) extends Utils {
  def stub: IO[T]

  val withSubCreated: IO[(WithId[T], CrudService[IO, T])] =
    for {
      client <- scopedClient
      crudService = service(client)
      expected <- stub
      createdStub <- crudService.create(expected)
    } yield (createdStub, crudService)

  s"The ${name} service" should {
    s"create ${name}s" in {
      val createIO = for {
        client <- scopedClient
        expected <- stub
        createdStub <- service(client).create(expected)
      } yield (createdStub, expected)

      def test(t: (WithId[T], T)): Assertion = {
        val (createdStub, expected) = t
        createdStub.model shouldBe expected
      }

      if (idempotent) {
        createIO.idempotently(test)
      } else {
        createIO.map(test)
      }
    }

    s"list ${name}s" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.list().compile.toList.idempotently(_ should contain (createdStub))
      }
    }

    s"get ${name}s" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.get(createdStub.id).valueShouldIdempotentlyBe(createdStub)
      }
    }

    s"delete a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.delete(createdStub.id).valueShouldIdempotentlyBe(())
      }
    }

    /*if (ev != null) {
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
    }*/

  }
}