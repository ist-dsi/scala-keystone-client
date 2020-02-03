package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.functor._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import pt.tecnico.dsi.keystone.models.regions.{Region, RegionWrapper, Regions}

class RegionService[F[_]: Sync](uri: Uri, token: Header)
                               (implicit client: Client[F]) extends BaseService {

  private val dsl = new Http4sClientDsl[F] with Methods {}
  import dsl._

  /**
    * Create a new region
    *
    * @param region the region
    * @return the newly created region
    */
  def create(region: Region): F[Region] = {
    client.expect[RegionWrapper](
      POST(RegionWrapper(region), uri, token)
    ).map(_.region)
  }

  /**
    * Get details for a region specified by id
    *
    * @param regionId the region id
    * @return the region
    */
  def get(regionId: String): F[Region] = {
    client.expect[RegionWrapper](
      GET(uri / regionId, token)
    ).map(_.region)
  }

  /**
    * Update a region
    *
    * @param region the region set to update
    * @return the updated region
    */
  def update(region: Region): F[Region] = {
    client.expect[RegionWrapper](
      PATCH(RegionWrapper(region), uri / region.id, token)
    ).map(_.region)
  }

  /**
    * Delete a region specified by id
    *
    * @param regionId the id of the region
    * @return the ActionResponse
    */
  def delete(regionId: String): F[Unit] =
    client.expect(DELETE(uri / regionId, token))

  /**
    * List regions
    *
    * @return a list of regions
    */
  def list(): F[Seq[Region]] = {
    client.expect[Regions](
      GET(uri, token)
    ).map(_.regions)
  }

}
