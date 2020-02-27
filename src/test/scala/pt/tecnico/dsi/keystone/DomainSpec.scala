package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.{Domain, User}

class DomainSpec extends Utils {
  "The domain service" should {
    "list domains" in idempotently { client =>
      for {
        domains <- client.domains.list().map(_.id).compile.toList
      } yield domains should contain (client.session.user.domainId)
    }

    "create domains" in idempotently { client =>
      for {
        domain <- client.domains.create(Domain(
          name = "domain-test",
          enabled = true,
          description = "Domain description"
        ))
      } yield domain.name shouldBe "domain-test"
    }

    "get domains" in idempotently { client =>
      for {
        domain <- client.domains.get(client.session.user.domainId)
      } yield domain.id shouldBe client.session.user.domainId
    }

    "delete a domain" in {
      for {
        client <- scopedClient
        domain <- client.domains.create(Domain(
          name = "domain-test",
          enabled = true,
          description = "Domain description"
        ))
        _ <- client.domains.delete(domain.id).idempotently(_ shouldBe ())
        domains <- client.users.getByName(domain.name).map(_.id).compile.toList
      } yield domains should not contain domain.id
    }
  }
}
