package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.keystone.models.{Region, WithId}

final class Regions[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CRUDService[F, Region](baseUri, "region", authToken) {
  /**
    * @param parentRegionId filters the response by a parent region, by ID.
    * @return a stream of regions filtered by the various parameters.
    */
  def list(parentRegionId: Option[String] = None): Stream[F, WithId[Region]] =
    list(Query.fromVector(Vector(
      "parent_region_id" -> parentRegionId,
    )))
}