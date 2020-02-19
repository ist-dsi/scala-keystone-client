package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.User

class UserSpec extends Utils {
  "The user service" should {
    "list users" in idempotently { client =>
      for {
        usernames <- client.users.list().map(_.name).compile.toList
      } yield usernames should contain (client.session.user.name)
    }

    "create users" in idempotently { client =>
      for {
        user <- client.users.create(User("teste", domainId = "default"))
        users <- client.users.getByName(user.name).map(_.model).compile.toList
      } yield users should contain (user.model)
    }

    "get a user" in idempotently { client =>
      for {
        usernames <- client.users.getByName("admin").map(_.name).compile.toList
      } yield usernames should contain ("admin")
    }

    /**
     * TODO: Cannot figure out how to delete (this fails, always)
     */
    "delete a user" in idempotently { client =>
      for {
        // Create user
        user <- client.users.create(User("teste2", domainId = "default"))
        _ <- client.users.delete(user.id) // TODO: This line breaks the test
        usernames <- client.users.getByName("teste2").map(_.id).compile.toList
      } yield assert(!usernames.contains(user.id))
    }
  }
}
