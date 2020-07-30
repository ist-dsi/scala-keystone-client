package pt.tecnico.dsi.openstack.keystone

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.Role
import pt.tecnico.dsi.openstack.keystone.services.Roles

class RoleSpec extends CrudSpec[Role, Role.Create, Role.Update]("role") {
  override def service: Roles[IO] = keystone.roles

  override def createStub(name: String): Role.Create = {
    // We cannot set a domainId because listing roles by default does not list roles from all domains
    // which causes the list test to fail
    Role.Create(name, Some("a description"), domainId = None)
  }
  override def compareCreate(create: Role.Create, model: Role): Assertion = {
    model.name shouldBe create.name
    model.description shouldBe create.description
    model.domainId shouldBe create.domainId
  }

  override def updateStub: Role.Update = Role.Update(Some(randomName()), Some(randomName()))
  override def compareUpdate(update: Role.Update, model: Role): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description
    model.domainId.isEmpty shouldBe true
  }

  def roleWithDomainResource: Resource[IO, Role] = {
    val create: IO[Role] = withRandomName { name =>
      service.create(createStub(name).copy(domainId = Some(keystone.session.user.domainId)))
    }
    Resource.make(create)(model => service.delete(model.id))
  }

  s"The ${name} service" should {
    s"list ${name}s in a domain" in roleWithDomainResource.use[IO, Assertion] { role =>
      keystone.roles.listByDomain(role.domainId.get).compile.toList.idempotently(_ should contain(role))
    }

    s"get ${name}s in a domain" in roleWithDomainResource.use[IO, Assertion] { role =>
      keystone.roles.get(role.name, role.domainId.get).idempotently(_ shouldBe role)
    }
  }
}