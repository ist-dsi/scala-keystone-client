package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.{Domain, Group, Role, WithId}

class DomainSpec extends CrudSpec[Domain]("domain", _.domains) with RoleAssignmentSpec[Domain] {
  def roleService = _.domains
  def stub = IO.pure(Domain(
    name = "domain-test",
    enabled = false,
    description = "Domain description",
  ))
  s"The ${name} service" should {
    s"forcefully delete an enabled ${name}" in {
      for {
        client <- scopedClient
        expected <- stub
        obj <- client.domains.create(expected.copy(enabled = true))
        result <- client.domains.delete(obj.id, force = true).valueShouldIdempotentlyBe(())
      } yield result
    }
  }
}