package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s.{Header, Uri}
import org.http4s.client.Client

class RoleAssignmentWithoutId[F[_]](base: RoleAssignmentService[F]) {
  def list(targetId: String) = base.list("", targetId)
  def assign(targetId: String, roleId: String) =  base.assign("", targetId, roleId)
  def check(targetId: String, roleId: String) =  base.check("", targetId, roleId)
  def delete(targetId: String, roleId: String)= base.delete("", targetId, roleId)
}

final class System[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends Service[F](authToken) {
  val uri: Uri = baseUri / "system"

  object roles {
    private val base = new RoleAssignmentService[F](uri, _, authToken)
    val users = new RoleAssignmentWithoutId(base("users"))
    val groups = new RoleAssignmentWithoutId(base("groups"))
  }
}
