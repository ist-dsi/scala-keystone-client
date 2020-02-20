package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.Credential

class CredentialSpec extends Utils {
  "The credential service" should {
    "list credentials" in idempotently { client =>
      for {
        userIds <- client.credentials.list().map(_.userId).compile.toList
      } yield userIds should contain (client.session.user.id)
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
        lastCredentialId <- client.credentials.list().compile.lastOrError.map(_.id)
        credential <- client.credentials.get(lastCredentialId)
      } yield credential.id shouldBe lastCredentialId
    }
  }
}
