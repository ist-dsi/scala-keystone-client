package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.http4s.Query
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.Service
import pt.tecnico.dsi.openstack.keystone.services.Services

class ServiceSpec extends CrudSpec[Service, Service.Create, Service.Update]("service") with EnableDisableSpec[Service] {
  override def service: Services[IO] = keystone.services

  override def getEnabled(model: Service): Boolean = model.enabled

  override def createStub(name: String): Service.Create = Service.Create(
    name,
    `type` = "openstack-should-validate-these",
    description = Some("a description")
  )
  override def compareCreate(create: Service.Create, model: Service): Assertion = {
    model.name shouldBe create.name
    model.`type` shouldBe create.`type`
    model.description shouldBe create.description
    model.enabled shouldBe create.enabled
  }
  override def createListQuery(name: String, create: Service.Create, repetitions: Int): Query =
    super.createListQuery(name, create, repetitions).withQueryParam("type", create.`type`)
  
  override def updateStub: Service.Update = Service.Update(
    name = Some(randomName()),
    `type` = Some(randomName()),
    description = Some(randomName()),
    enabled = Some(false)
  )
  override def compareUpdate(update: Service.Update, model: Service): Assertion = {
    model.name shouldBe update.name.value
    model.`type` shouldBe update.`type`.value
    model.description shouldBe update.description
    model.enabled shouldBe update.enabled.value
  }
}