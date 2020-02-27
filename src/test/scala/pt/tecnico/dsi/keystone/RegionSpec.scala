package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.{Endpoint, Group}

class RegionSpec extends Utils {
  "The region service" should {

/*
    // TODO: If you take this counter off, tests will explode with conflict errors
    var i = 1500
    def genRegion = scopedClient.map(client => {
      i += 1
      Group(
        name = "test-create-group" + i,
        description = "test-create-desc" + i,
        domainId = client.session.user.domainId
      )

      Endpoint(

      )
    })

    "create regions" in idempotently { client =>
      for {
        expected <- genGroup
        received <- client.regions.create(expected)
      } yield expected should be (received.model)
    }

    "list regions" in idempotently { client =>
      for {
        expected <- genGroup
        created <- client.regions.create(expected)
        regions <- client.regions.list().compile.toList
      } yield regions should contain (created)
    }

    "get regions" in idempotently { client =>
      for {
        expected <- genGroup
        created <- client.regions.create(expected)
        received <- client.regions.get(created.id)
      } yield created should be (received)
    }
*/

  }
}
