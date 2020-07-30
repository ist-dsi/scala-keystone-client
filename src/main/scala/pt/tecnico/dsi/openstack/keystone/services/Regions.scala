package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Status.BadRequest
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Region

final class Regions[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Region, Region.Create, Region.Update](baseUri, "region", authToken) {
  /**
    * @param parentRegionId filters the response by a parent region, by ID.
    * @return a stream of regions filtered by the various parameters.
    */
  def list(parentRegionId: Option[String] = None): Stream[F, Region] =
    list(Query.fromVector(Vector(
      "parent_region_id" -> parentRegionId,
    )))

  override def create(create: Region.Create, extraHeaders: Header*): F[Region] = createHandleConflict(create, extraHeaders:_*) {
    create.id match {
      case Some(id) =>
        // We got a Conflict and we have a domainId so we can find the existing User since it must already exist
        apply(id).flatMap { existingRegion =>
          if (existingRegion.description != create.description || existingRegion.parentRegionId != create.parentRegionId) {
            val updated = Region.Update(create.description, create.parentRegionId)
            update(existingRegion.id, updated, extraHeaders:_*)
          } else {
            Sync[F].pure(existingRegion)
          }
        }
      case None =>
        // This will never happen, unless openstack generates repeated ids.
        // TODO: what is the correct status to return?
        Sync[F].raiseError(UnexpectedStatus(BadRequest))
    }
  }
}