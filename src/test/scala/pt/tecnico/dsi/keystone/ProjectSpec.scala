package pt.tecnico.dsi.keystone

import cats.effect.IO
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
      } yield project.model.name shouldBe "project-test"
    }

    "get projects" in idempotently { client =>
      for {
        list <- client.projects.list().compile.toList
        project <- client.projects.get(list.last.id)
      } yield project.id shouldBe list.last.id
    }
  }
}
