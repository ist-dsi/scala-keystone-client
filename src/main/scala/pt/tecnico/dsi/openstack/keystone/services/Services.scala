package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Service

final class Services[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Service, Service.Create, Service.Update](baseUri, "service", authToken)
  with EnableDisableEndpoints[F, Service] {
  /**
    *  @param `type` Filters the response by a service type.
    * @return a stream of services filtered by the various parameters.
    */
  def list(`type`: Option[String] = None): Stream[F, Service] =
    list(Query.fromVector(Vector(
      "type" -> `type`,
    )))

  override protected def updateEnable(id: String, enabled: Boolean): F[Service] = update(id, Service.Update(enabled = Some(enabled)))

  // TODO: creating a service is not an idempotent operation
}