package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.{Group, Project}

class ProjectSpec extends CRUDSpec[Project]("project", _.projects) {
  def stub = IO.pure(Project(
    name = "test-group",
    description = "test-desc",
    domainId = "default",
  ))
}