package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.{Endpoint, Interface, Region, Service}

class EndpointSpec extends CrudSpec[Endpoint]("endpoint", _.endpoints) {

  def stub = for {
    client <- scopedClient
    service <- client.services.create(Service("region-spec-service", "compute", "whatever"))
    region <- client.regions.create(Region(description = "region description"))
  } yield Endpoint(
    interface = Interface.Public,
    regionId = region.id,
    serviceId = service.id,
    url = "http://localhost:5042/example/test",
  )

}