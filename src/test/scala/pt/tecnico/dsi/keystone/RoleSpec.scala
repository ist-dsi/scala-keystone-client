package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.Role

class RoleSpec extends CrudSpec[Role]("role", _.roles) {
  def stub = IO.pure(Role(
    name = "role-without-domain",
    description = Some("some-description"),
    domainId = None, // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
  ))

  def stubWithDomain(client: KeystoneClient[IO]) = IO.pure {
    Role(
      name = "role-with-domain",
      description = Some("some-description"),
      domainId = Some(client.session.user.domainId),
    )
  }

  s"The ${name} service" should {
    s"create ${name}s with domainId" in idempotently { client =>
      for {
        expected <- stubWithDomain(client)
        actual <- client.roles.create(expected)
      } yield actual.model shouldBe expected
    }

    s"list ${name}s in a domain" in {
      for {
        client <- scopedClient
        expected <- stubWithDomain(client)
        obj <- client.roles.create(expected)
        isIdempotent <- client.roles.listByDomain(expected.domainId.get).compile.toList.idempotently(_ should contain(obj))
      } yield isIdempotent
    }

    s"get ${name}s in a domain" in {
      for {
        client <- scopedClient
        expected <- stubWithDomain(client)
        obj <- client.roles.create(expected)
        isIdempotent <- client.roles.get(expected.name, expected.domainId.get).idempotently(_ shouldBe obj)
      } yield isIdempotent
    }
  }
}