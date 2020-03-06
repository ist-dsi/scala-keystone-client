package pt.tecnico.dsi.keystone

import cats.effect.IO
import org.http4s.Uri
import pt.tecnico.dsi.keystone.models.{Endpoint, Interface, Project, Region, Service, User}

class EndpointSpec extends CRUDSpec[Endpoint]("endpoint", _.endpoints) {

  def stub = for {
      client <- scopedClient
      service <- client.services.create(Service("region-spec-service", "compute", "whatever"))
      region <- client.regions.create(Region(description = "region description"))
  } yield Endpoint(
    interface = Interface.Public,
    regionId = region.id,
    serviceId = service.id,
    url = Uri.unsafeFromString("http://localhost:5042/example/test")
  )

}