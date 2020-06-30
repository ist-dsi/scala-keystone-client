package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.models.Credential

final class Credentials[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CrudService[F, Credential](baseUri, "credential", authToken) {

  /**
    * @param userId filters the response by a user ID.
    * @param `type` The credential type, such as ec2 or cert. The implementation determines the list of supported types.
    * @return a stream of services filtered by the various parameters.
    */
  def list(userId: Option[String] = None, `type`: Option[String] = None): Stream[F, WithId[Credential]] =
    list(Query.fromVector(Vector(
      "user_id" -> userId,
      "type" -> `type`,
    )))

  override def create(credential: Credential): F[WithId[Credential]] = super.createHandleConflict(credential) { _ =>
    list(userId = Some(credential.userId), `type` = Some("ec2"))
      .filter(c => c.projectId == credential.projectId).compile.lastOrError
      .flatMap(existingCredential => update(existingCredential.id, credential))
  }
}