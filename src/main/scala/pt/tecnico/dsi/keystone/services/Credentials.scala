package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.keystone.models.{Credential, WithId}

class Credentials[F[_]: Sync](baseUri: Uri, authToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Credential](baseUri, "credential", authToken) {

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

  override def create(credential: Credential): F[WithId[Credential]] = createHandleConflict(credential) { _ =>
    list(userId = Some(credential.userId), `type` = Some("ec2"))
      .filter(c => c.projectId == credential.projectId).compile.lastOrError
      .flatMap(existingCredential => update(existingCredential.id, credential))
  }
}