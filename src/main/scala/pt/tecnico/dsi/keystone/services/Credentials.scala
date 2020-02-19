package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.{Credential, WithId}

class Credentials[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Credential](baseUri, "credential", subjectToken) {

    override def create(credential: Credential): F[WithId[Credential]] = createHandleConflict(credential) {
      list().filter(x => x.model == credential).compile.lastOrError
    }
}