package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.User
import pt.tecnico.dsi.openstack.keystone.services.Users

class UserSpec extends CrudSpec[User, User.Create, User.Update]("user") with EnableDisableSpec[User] {
  override def service: Users[IO] = keystone.users

  override def getEnabled(model: User): Boolean = model.enabled

  override def createStub(name: String): User.Create = User.Create(name, Some(randomName()))
  override def compareCreate(create: User.Create, model: User): Assertion = {
    model.name shouldBe create.name
    model.defaultProjectId shouldBe create.defaultProjectId
    model.enabled shouldBe create.enabled
    // Since we didn't specified the domainId, and the token we used to authenticate isn't domain-scoped
    // the user will be created with domainId = default
    model.domainId shouldBe keystone.session.scopedDomainId()
  }

  override def updateStub: User.Update = User.Update(Some(randomName()), Some(randomName()), Some(randomName()), Some(false))
  override def compareUpdate(update: User.Update, model: User): Assertion = {
    model.name shouldBe update.name.value
    model.defaultProjectId shouldBe update.defaultProjectId
    model.enabled shouldBe update.enabled.value
  }
}