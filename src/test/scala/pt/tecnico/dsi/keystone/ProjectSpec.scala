package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.{Group, Project}

class ProjectSpec extends CrudSpec[Project]("project", _.projects) with RoleAssignmentSpec[Project] {
  def roleService = _.projects
  def stub = IO.pure(Project(
    name = "test-group",
    description = "test-desc",
    domainId = "default",
  ))
}