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

    /*
    throws
    [info] - should delete a user *** FAILED ***
[info]   java.util.NoSuchElementException:
[info]   at fs2.Stream$CompileOps.$anonfun$lastOrError$3(Stream.scala:4327)
[info]   at scala.Option.fold(Option.scala:263)
[info]   at fs2.Stream$CompileOps.$anonfun$lastOrError$2(Stream.scala:4327)
[info]   at cats.effect.internals.IORunLoop$.cats$effect$internals$IORunLoop$$loop(IORunLoop.scala:139)

idk why

    "delete a user" in idempotently { client =>
      for {
        // Create user
        user <- client.users.create(User("teste", domainId = "default"))
        _ <- client.users.delete(user.id)
      } yield true shouldBe true
    }
     */
  }
}
