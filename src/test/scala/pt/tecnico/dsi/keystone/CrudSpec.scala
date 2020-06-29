package pt.tecnico.dsi.keystone

import cats.effect.IO
import io.circe.Codec
import pt.tecnico.dsi.keystone.models.WithId
import pt.tecnico.dsi.keystone.services.CrudService

abstract class CrudSpec[T](val name: String, val service: KeystoneClient[IO] => CrudService[IO, T])
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
      for {
        client <- scopedClient
        expected <- stub
        result <- service(client).create(expected).idempotently(_.model shouldBe expected)
      } yield result
    }

    s"list ${name}s" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.list().compile.toList.idempotently(_ should contain (createdStub))
      }
    }

    s"get ${name}s" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.get(createdStub.id).idempotently(_ shouldBe createdStub)
      }
    }

    s"delete a ${name}" in {
      withSubCreated.flatMap { case (createdStub, service) =>
        service.get(createdStub.id).idempotently(_ shouldBe ())
      }
    }
  }
}