package pt.tecnico.dsi.keystone

import pt.tecnico.dsi.keystone.models.Credential

class CredentialSpec extends CrudSpec[Credential]("credential", _.credentials) {
  def stub = scopedClient.map { client =>
    Credential(
      access = "181920",
      secret = "secretKey",
      projectId = "731fc6f265cd486d900f16e84c5cb594",
      userId = client.session.user.id
    )
  }
}