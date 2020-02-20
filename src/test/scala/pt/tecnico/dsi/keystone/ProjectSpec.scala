package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.Project

class ProjectSpec extends Utils {
  "The project service" should {
    "list projects" in idempotently { client =>
      for {
        projects <- client.projects.list().compile.toList
      } yield assert(projects.nonEmpty)
    }

    "create projects" in idempotently { client =>
      for {
        project <- client.projects.create(Project(
          name = "project-test",
          description = "new project",
          domainId = "default",
        ))
      } yield project.name shouldBe "project-test"
    }

    "get projects" in idempotently { client =>
      for {
        lastProjectId <- client.projects.list().compile.lastOrError.map(_.id)
        project <- client.projects.get(lastProjectId)
      } yield project.id shouldBe lastProjectId
    }
  }
}
