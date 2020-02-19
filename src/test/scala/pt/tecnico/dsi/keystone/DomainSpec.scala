package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.Domain

class DomainSpec extends Utils {
  "The domain service" should {
    "list domains" in idempotently { client =>
      for {
        domains <- client.projects.list().compile.toList
      } yield assert(domains.nonEmpty)
    }

    "create domains" in idempotently { client =>
      for {
        domain <- client.domains.create(Domain(
          name = "domain-test",
          enabled = true,
          description = "Domain description"
        ))
      } yield domain.model.name shouldBe "domain-test"
    }

    "get domains" in idempotently { client =>
      for {
        domain <- client.domains.get(client.session.user.domainId)
      } yield domain.id shouldBe client.session.user.domainId
    }
  }
}
