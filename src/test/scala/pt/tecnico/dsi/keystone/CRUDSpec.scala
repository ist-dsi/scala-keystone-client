package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.WithEnabled
import pt.tecnico.dsi.keystone.services.CRUDService

abstract class CRUDSpec[T]
  (name: String, service: KeystoneClient[IO] => CRUDService[IO, T], idempotent: Boolean = true)
  (implicit ev: T <:< WithEnabled[T] = null) extends Utils {

  def stub: IO[T]

  s"The ${name} service" should {

    if (idempotent) {
      s"create ${name}s" in idempotently { client =>
        for {
          expected <- stub
          actual <- service(client).create(expected)
        } yield actual.model should be (expected)
      }
    } else {
      s"create ${name}s" in {
        for {
          client <- scopedClient
          expected <- stub
          actual <- service(client).create(expected)
        } yield actual.model should be (expected)
      }
    }

    s"list ${name}s" in {
      for {
        client <- scopedClient
        expected <- stub
        obj <- service(client).create(expected)
        isIdempotent <- service(client).list().compile.toList.idempotently(_ should contain (obj))
      } yield isIdempotent
    }

    s"get ${name}s" in {
      for {
        client <- scopedClient
        expected <- stub
        obj <- service(client).create(expected)
        isIdempotent <- service(client).get(obj.id).valueShouldIdempotentlyBe(obj)
      } yield isIdempotent
    }

    s"delete a ${name}" in {
      for {
        client <- scopedClient
        expected <- stub
        obj <- service(client).create(expected)
        result <- service(client).delete(obj.id).valueShouldIdempotentlyBe(())
      } yield result
    }

    if (ev != null) {
      s"enable a ${name}" in {
        for {
          client <- scopedClient
          expected <- stub
          obj <- service(client).create(expected)
          result <- {
            for {
              _ <- service(client).enable(obj.id)
              get <- service(client).get(obj.id)
            } yield get.model.enabled
            }.valueShouldIdempotentlyBe(true)
        } yield result
      }

      s"disable a ${name}" in {
        for {
          client <- scopedClient
          expected <- stub
          obj <- service(client).create(expected)
          result <- {
            for {
              _ <- service(client).disable(obj.id)
              get <- service(client).get(obj.id)
            } yield get.model.enabled
          }.valueShouldIdempotentlyBe(false)
        } yield result
      }
    }

  }
}
