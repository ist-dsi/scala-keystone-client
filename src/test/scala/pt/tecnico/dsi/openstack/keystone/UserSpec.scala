package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import pt.tecnico.dsi.openstack.keystone.models.User

class UserSpec extends CrudSpec[User]("user", _.users) with EnableDisableSpec[User] {
  def stub = IO.pure(User(
    name = "test-user",
    domainId = "default"
  ))
}