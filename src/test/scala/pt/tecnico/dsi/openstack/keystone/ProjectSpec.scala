package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import pt.tecnico.dsi.openstack.keystone.models.Project

class ProjectSpec extends CrudSpec[Project]("project", _.projects) with RoleAssignmentSpec[Project] with EnableDisableSpec[Project] {
  def roleService = _.projects
  def stub = IO.pure(Project(
    name = "test-group",
    description = "test-desc",
    domainId = "default",
  ))
}