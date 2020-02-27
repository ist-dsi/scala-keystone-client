package pt.tecnico.dsi.keystone

import org.http4s.Uri
import pt.tecnico.dsi.keystone.models.{Endpoint, Interface, User}

class EndpointSpec extends Utils {
  "The endpoint service" should {

    // TODO: We need to create a region and service to use here
    def endpointStub = Endpoint(
      interface = Interface.Public,
      regionId = "default",
      serviceId = "default",
      url = Uri.unsafeFromString("localhost:5042/example/test")
    )

    "create endpoints" in idempotently { client =>
      for {
        received <- client.endpoints.create(endpointStub)
      } yield endpointStub should be (received.model)
    }

    "list endpoints" in idempotently { client =>
      for {
        expected <- client.endpoints.create(endpointStub)
        endpoints <- client.endpoints.list().map(_.id).compile.toList
      } yield endpoints should contain (expected.id)
    }

    "get endpoints" in idempotently { client =>
      for {
        expected <- client.endpoints.create(endpointStub)
        received <- client.endpoints.get(expected.id)
      } yield expected should be (received)
    }

    "delete endpointStub" in {
      for {
        client <- scopedClient
        endpoint <- client.endpoints.create(endpointStub)
        _ <- client.users.delete(endpoint.id).idempotently(_ shouldBe ())
        endpoints <- client.endpoints.list().map(_.id).compile.toList
      } yield endpoints should not contain endpoint.id
    }
  }
}
