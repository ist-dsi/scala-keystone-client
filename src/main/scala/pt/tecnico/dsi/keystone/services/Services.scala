package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.Service

class Services[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F])
  extends CRUDService[F, Service](baseUri, "service", subjectToken)
