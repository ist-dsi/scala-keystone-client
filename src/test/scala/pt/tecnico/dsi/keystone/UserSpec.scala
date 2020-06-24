package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.User

class UserSpec extends CrudSpec[User]("user", _.users) {
  def stub = IO.pure(User(
    name = "test-user",
    domainId = "default"
  ))
}