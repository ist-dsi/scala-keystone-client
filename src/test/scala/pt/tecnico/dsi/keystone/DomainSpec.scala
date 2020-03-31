package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.Domain

class DomainSpec extends CRUDSpec[Domain]("domain", _.domains) {
  def stub = IO.pure(Domain(
    name = "domain-test",
    enabled = false,
    description = "Domain description",
  ))

  def enabledStub = IO.pure(Domain(
    name = "enabled-domain",
    enabled = true,
    description = "A domain that is enabled",
  ))

  s"The ${name} service" should {
    s"forcefully delete an enabled ${name}" in {
      for {
        client <- scopedClient
        expected <- enabledStub
        obj <- client.domains.create(expected)
        result <- client.domains.delete(obj.id, force = true).valueShouldIdempotentlyBe(())
      } yield result
    }
  }
}