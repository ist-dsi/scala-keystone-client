package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.models.Endpoint

final class Endpoints[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CrudService[F, Endpoint](baseUri, "endpoint", authToken)
  with EnableDisableEndpoints[F, Endpoint] {
  /**
    * @param interface filters the response by an interface.
    * @param serviceId filters the response by a domain ID.
    * @return a stream of endpoints filtered by the various parameters.
    */
  def list(interface: Option[String] = None, serviceId: Option[String] = None): Stream[F, WithId[Endpoint]] =
    list(Query.fromVector(Vector(
      "interface" -> interface,
      "service_ id" -> serviceId,
    )))
}
