package pt.tecnico.dsi.openstack.keystone

import cats.effect.{IO, Resource}
import org.http4s.client.UnexpectedStatus
import org.scalatest.{Assertion, EitherValues}
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

abstract class CrudSpec[Model <: Identifiable, Create, Update](val name: String) extends Utils with EitherValues {
  def service: CrudService[IO, Model, Create, Update]

  def createStub(name: String): Create
  def compareCreate(create: Create, model: Model): Assertion

  def updateStub: Update
  def compareUpdate(update: Update, model: Model): Assertion

  def resource: Resource[IO, Model] = resourceCreator(service)(createStub)

  s"The $name service" should {
    s"list ${name}s" in resource.use[IO, Assertion] { model =>
      service.list().compile.toList.idempotently(_ should contain (model))
    }

    s"create ${name}s" in {
      val stub = createStub(randomName())
      for {
        first <- service.create(stub)
        result <- service.create(stub).idempotently(compareCreate(stub, _), repetitions = 2)
        // TODO: list and ensure only 1 instance was created
        _ <- service.delete(first.id)
      } yield result
    }

    s"get ${name}s (existing id)" in resource.use[IO, Assertion] { model =>
      service.get(model.id).idempotently(_.value shouldBe model)
    }
    s"get ${name}s (non-existing id)" in {
      service.get("non-existing-id").idempotently(_ shouldBe None)
    }

    s"apply ${name}s (existing id)" in resource.use[IO, Assertion] { model =>
      service.apply(model.id).idempotently(_ shouldBe model)
    }
    s"apply ${name}s (non-existing id)" in {
      service.apply("non-existing-id").attempt.idempotently(_.left.value shouldBe a [UnexpectedStatus])
    }

    s"update ${name}s" in resource.use[IO, Assertion] { model =>
      val dummyUpdate = updateStub
      service.update(model.id, dummyUpdate).idempotently(compareUpdate(dummyUpdate, _))
    }

    s"delete a ${name}" in resource.use[IO, Assertion] { model =>
      service.delete(model.id).idempotently(_ shouldBe ())
    }
  }
}