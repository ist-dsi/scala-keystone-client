package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.Group

class GroupSpec extends Utils {
  "The group service" should {

    // TODO: Conflict errors everywhere if you run the tests with the same groups twice
    var i = 1500
    def genGroup = scopedClient.map(client => {
      i += 1
      Group(
        name = "test-group" + i,
        description = "test-desc" + i,
        domainId = client.session.user.domainId
      )
    })

    val group1 = genGroup
    "create groups" in idempotently { client =>
      for {
        expected <- group1
        received <- client.groups.create(expected)
      } yield expected should be (received.model)
    }

    val group2 = genGroup
    "list groups" in idempotently { client =>
      for {
        expected <- group2
        created <- client.groups.create(expected)
        groups <- client.groups.list().compile.toList
      } yield groups should contain (created)
    }

    val group3 = genGroup
    "get groups" in idempotently { client =>
      for {
        expected <- group3
        created <- client.groups.create(expected)
        received <- client.groups.get(created.id)
      } yield created should be (received)
    }

  }
}
