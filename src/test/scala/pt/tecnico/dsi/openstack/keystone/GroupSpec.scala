package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.Group
import pt.tecnico.dsi.openstack.keystone.services.{Groups, domainIdFromScope}

class GroupSpec extends CrudSpec[Group, Group.Create, Group.Update]("group") {
  override def service: Groups[IO] = keystone.groups

  override def createStub(name: String): Group.Create = Group.Create(name, Some("a description"))
  override def compareCreate(create: Group.Create, model: Group): Assertion = {
    model.name shouldBe create.name
    model.description shouldBe create.description
    // Since we didn't specified the domainId, and the token we used to authenticate isn't domain-scoped
    // the group will be created with domainId = default
    model.domainId shouldBe domainIdFromScope(keystone.session.scope)
  }

  override def updateStub: Group.Update = Group.Update(name = Some(randomName()), Some("a better and improved description"))
  override def compareUpdate(update: Group.Update, model: Group): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description
  }
}
