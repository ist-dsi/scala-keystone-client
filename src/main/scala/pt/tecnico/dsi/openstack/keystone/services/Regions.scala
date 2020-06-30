package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.models.Region

final class Regions[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CrudService[F, Region](baseUri, "region", authToken) {
  /**
    * @param parentRegionId filters the response by a parent region, by ID.
    * @return a stream of regions filtered by the various parameters.
    */
  def list(parentRegionId: Option[String] = None): Stream[F, WithId[Region]] =
    list(Query.fromVector(Vector(
      "parent_region_id" -> parentRegionId,
    )))
}