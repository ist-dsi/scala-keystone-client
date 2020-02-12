package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.Endpoint

class Endpoints[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Endpoint](baseUri, "endpoint", subjectToken)
