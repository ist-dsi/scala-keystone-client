package pt.tecnico.dsi.openstack.keystone

import cats.effect.{IO, Resource}
import org.http4s.Query
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.{Endpoint, Interface, Region, Service}
import pt.tecnico.dsi.openstack.keystone.services.Endpoints

class EndpointSpec extends CrudSpec[Endpoint, Endpoint.Create, Endpoint.Update]("endpoint") with EnableDisableSpec[Endpoint] {
  override def service: Endpoints[IO] = keystone.endpoints

  val stubsResource: Resource[IO, (String, String)] = for {
    region <- resourceCreator(keystone.regions)(name => Region.Create(name))
    service <- resourceCreator(keystone.services)(Service.Create(_, "random-type"))
  } yield (service.id, region.id)

  // This way we use the same Service and Region for every test, and make the logs smaller and easier to debug.
  val ((serviceId, regionId), stubsDelete) = stubsResource.allocated.unsafeRunSync()
  override protected def afterAll(): Unit = {
    stubsDelete.unsafeRunSync()
    super.afterAll()
  }

  override def getEnabled(model: Endpoint): Boolean = model.enabled

  override def createStub(name: String): Endpoint.Create =
    Endpoint.Create(Interface.Public, "http://localhost:5042/example/test", serviceId, regionId)
  override def compareCreate(create: Endpoint.Create, model: Endpoint): Assertion = {
    model.interface shouldBe create.interface
    model.url shouldBe create.url
    model.serviceId shouldBe create.serviceId
    model.regionId shouldBe create.regionId
    model.enabled shouldBe create.enabled
  }
  
  override def createListQuery(name: String, create: Endpoint.Create, repetitions: Int): Query = Query.fromPairs(
    "interface" -> create.interface.toString.toLowerCase,
    "service_id" -> create.serviceId,
    "region_id" -> create.regionId,
  )
  
  override def updateStub: Endpoint.Update =
    Endpoint.Update(Some(Interface.Admin), Some("http://localhost:5042/example/updated"), enabled = Some(false))
  override def compareUpdate(update: Endpoint.Update, model: Endpoint): Assertion = {
    model.interface shouldBe update.interface.value
    model.url shouldBe update.url.value
    model.enabled shouldBe update.enabled.value
  }
}