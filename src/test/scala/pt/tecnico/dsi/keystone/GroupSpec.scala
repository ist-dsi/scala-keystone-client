package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.Group

class GroupSpec extends CRUDSpec[Group]("group", _.groups) {
  def stub = IO.pure(Group(
    name = "test-group",
    description = "test-desc",
    domainId = "default"
  ))
}
