package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.models.Service

final class Services[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CrudService[F, Service](baseUri, "service", authToken)
  with EnableDisableEndpoints[F, Service] {
  /**
    *  @param `type` Filters the response by a service type.
    * @return a stream of services filtered by the various parameters.
    */
  def list(`type`: Option[String] = None): Stream[F, WithId[Service]] =
    list(Query.fromVector(Vector(
      "type" -> `type`,
    )))
}