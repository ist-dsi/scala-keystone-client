package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.Credential

class CredentialSpec extends Utils {
  "The credential service" should {
    "list credentials" in idempotently { client =>
      for {
        domains <- client.credentials.list().map(_.model.userId).compile.toList
      } yield assert(domains.contains(client.session.user.id))
    }

    /**
     * TODO: Make this idempotent
     */
    "create credentials" in idempotently { client =>
      for {
        credential <- client.credentials.create(
          Credential(
            access = "181920",
            secret = "secretKey",
            projectId = "731fc6f265cd486d900f16e84c5cb594",
            userId = client.session.user.id
          )
        )
      } yield credential.secret shouldBe "secretKey"
    }

    "get credentials" in idempotently { client =>
      for {
        list <- client.credentials.list().compile.toList
        credential <- client.credentials.get(list.last.id)
      } yield credential.id shouldBe list.last.id
    }
  }
}
