package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.services.CRUDService

abstract class CRUDSpec[T]
  (name: String, service: KeystoneClient[IO] => CRUDService[IO, T], idempotent: Boolean = true) extends Utils {

  def stub: IO[T]

  s"The ${name} service" should {

    if (idempotent) {
      s"create ${name}s" in idempotently { client =>
        for {
          expected <- stub
          actual <- stub.flatMap(service(client).create)
        } yield actual.model should be (expected)
      }
    } else {
      s"create ${name}s" in {
        for {
          client <- scopedClient
          expected <- stub
          actual <- stub.flatMap(service(client).create)
        } yield actual.model should be (expected)
      }
    }

    s"list ${name}s" in {
      for {
        client <- scopedClient
        obj <- stub.flatMap(service(client).create)
        isIdempotent <- service(client).list().compile.toList.idempotently(_ should contain (obj))
      } yield isIdempotent
    }

    s"get ${name}s" in {
      for {
        client <- scopedClient
        obj <- stub.flatMap(service(client).create)
        isIdempotent <- service(client).get(obj.id).valueShouldIdempotentlyBe(obj)
      } yield isIdempotent
    }

    s"delete a ${name}" in {
      for {
        client <- scopedClient
        domain <- stub.flatMap(service(client).create)
        result <- service(client).delete(domain.id).valueShouldIdempotentlyBe(())
      } yield result
    }

  }
}
