package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.Domain

class DomainSpec extends CRUDSpec[Domain]("domain", _.domains) {
  def stub = IO.pure(Domain(
    name = "domain-test",
    enabled = false,
    description = "Domain description",
  ))
}