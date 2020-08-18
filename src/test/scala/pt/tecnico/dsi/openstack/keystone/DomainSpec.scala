package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.Domain
import pt.tecnico.dsi.openstack.keystone.services.{Domains, RoleAssignment}

final class DomainSpec extends CrudSpec[Domain, Domain.Create, Domain.Update]("domain")
  with RoleAssignmentSpec[Domain] {
  override def service: Domains[IO] = keystone.domains
  def roleService(model: Domain): RoleAssignment[IO] = service.on(model)

  override def createStub(name: String): Domain.Create = {
    // We set enabled = false so the delete test can pass
    Domain.Create(name, Some("a description"), enabled = false)
  }
  override def compareCreate(create: Domain.Create, model: Domain): Assertion = {
    model.name shouldBe create.name
    model.description shouldBe create.description
    model.enabled shouldBe create.enabled
  }

  override def updateStub: Domain.Update = Domain.Update(name = Some(randomName()), Some("a better and improved description"))
  override def compareUpdate(update: Domain.Update, model: Domain): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description
    model.enabled shouldBe false // We didn't change it from the create
  }

  s"The $name service" should {
    s"forcefully delete an enabled $name" in {
       for {
        obj <- keystone.domains.create(createStub(randomName()).copy(enabled = true))
        result <- keystone.domains.delete(obj.id, force = true).idempotently(_ shouldBe ())
      } yield result
    }
    s"enable a $name" in resource.use[IO, Assertion] { model =>
      for {
        result <- service.enable(model.id).idempotently(_.enabled shouldBe true)
        _ <- service.disable(model.id) // We need to disable the domain, otherwise the Resource cleanup will fail with forbidden
      } yield result
    }

    s"disable a $name" in resource.use[IO, Assertion] { model =>
      service.disable(model.id).idempotently(_.enabled shouldBe false)
    }
  }
}