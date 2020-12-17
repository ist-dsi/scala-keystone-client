package pt.tecnico.dsi.openstack.keystone

import scala.annotation.nowarn
import cats.derived.ShowPretty
import cats.effect.{IO, Resource}
import cats.implicits._
import org.http4s.Query
import org.scalatest.{Assertion, EitherValues, OptionValues}
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

abstract class CrudSpec[Model <: Identifiable: ShowPretty, Create, Update](val name: String)
  extends Utils with EitherValues with OptionValues {
  def service: CrudService[IO, Model, Create, Update]

  def createStub(name: String): Create
  def compareCreate(create: Create, model: Model): Assertion
  def createListQuery(name: String, @nowarn create: Create, repetitions: Int): Query =
    Query.fromPairs("name" -> name, "limit" -> repetitions.toString)
  
  // This exists just because of Regions
  def filterFunction(@nowarn create: Create, @nowarn model: Model): Boolean = true
  
  def updateStub: Update
  def compareUpdate(update: Update, model: Model): Assertion

  def resource: Resource[IO, Model] = resourceCreator(service)(createStub)
  
  s"The $name service" should {
    s"list ${name}s" in resource.use[IO, Assertion] { model =>
      service.list().idempotently(_ should contain (model))
    }
    
    // TODO: this is an easy test for the createOrUpdate. We should implement another one which creates a Model, updates it, and createsOrUpdates it.
    s"createOrUpdate ${name}s" in {
      val name = randomName()
      val create = createStub(name)
      val repetitions = 3
      
      for {
        _ <- service.createOrUpdate(create).idempotently(compareCreate(create, _), repetitions)
        // This does not work for regions because list does not accept the name filter
        list <- service.list(createListQuery(name, create, repetitions)).map(_.filter(m => filterFunction(create, m)))
        _ <- list.parTraverse_(service.delete(_))
      } yield list.size shouldBe 1
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
      val id = "non-existing-id"
      service.apply(id).attempt.idempotently { either =>
        either.left.value shouldBe a[NoSuchElementException]
        val exception = either.left.value.asInstanceOf[NoSuchElementException]
        exception.getMessage should include (s"Could not find $name")
      }
    }

    s"update ${name}s" in resource.use[IO, Assertion] { model =>
      val dummyUpdate = updateStub
      service.update(model.id, dummyUpdate).idempotently(compareUpdate(dummyUpdate, _))
    }

    s"delete ${name}s" in resource.use[IO, Assertion] { model =>
      service.delete(model.id).idempotently(_ shouldBe ())
    }
    
    s"show ${name}s" in resource.use[IO, Assertion] { model =>
      //This line is a fail fast mechanism, and prevents false positives from the linter
      println(show"$model")
      IO("""show"$model"""" should compile): @nowarn
    }
  }
}