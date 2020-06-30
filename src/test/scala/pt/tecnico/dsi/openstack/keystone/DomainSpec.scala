package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import pt.tecnico.dsi.openstack.keystone.models.Domain

class DomainSpec extends CrudSpec[Domain]("domain", _.domains) with RoleAssignmentSpec[Domain] with EnableDisableSpec[Domain] {
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
        result <- client.domains.delete(obj.id, force = true).idempotently(_ shouldBe ())
      } yield result
    }
  }
}