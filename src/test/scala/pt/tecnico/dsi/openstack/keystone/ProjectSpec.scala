package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.Project
import pt.tecnico.dsi.openstack.keystone.services.{Projects, RoleAssignment}

class ProjectSpec extends CrudSpec[Project, Project.Create, Project.Update]("project")
  with RoleAssignmentSpec[Project] with EnableDisableSpec[Project] {
  override def service: Projects[IO] = keystone.projects
  def roleService(model: Project): RoleAssignment[IO] = service.on(model)
  
  override def getEnabled(model: Project): Boolean = model.enabled

  override def createStub(name: String): Project.Create = Project.Create(name, Some("a description"), tags = List("a", "b", "c"))
  override def compareCreate(create: Project.Create, model: Project): Assertion = {
    model.name shouldBe create.name
    model.description shouldBe create.description
    model.isDomain shouldBe create.isDomain
    model.enabled shouldBe create.enabled
    // Since we didn't specified the domainId, and the token we used to authenticate isn't domain-scoped
    // the project will be created with domainId = default, which will cause the parentId to be default as well
    model.domainId shouldBe keystone.session.scopedDomainId()
    model.parentId shouldBe keystone.session.scopedDomainId()
    model.tags shouldBe create.tags
  }

  override def updateStub: Project.Update = Project.Update(Some(randomName()), Some(randomName()), Some(false), Some(List.empty))
  override def compareUpdate(update: Project.Update, model: Project): Assertion = {
    model.name shouldBe update.name.value
    model.description shouldBe update.description
    model.enabled shouldBe update.enabled.value
    model.tags shouldBe update.tags.value
  }
}
