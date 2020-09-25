package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Status.BadRequest
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Region, Session}

final class Regions[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Region, Region.Create, Region.Update](baseUri, "region", session.authToken) {
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
      case Some(id) => apply(id).flatMap { existing =>
        if (existing.description != create.description || existing.parentRegionId != create.parentRegionId) {
          val updated = Region.Update(
            if (existing.description != create.description) create.description else None,
            if (existing.parentRegionId != create.parentRegionId) create.parentRegionId else None,
          )
          update(existing.id, updated, extraHeaders:_*)
        } else {
          Sync[F].pure(existing)
        }
      }
      case None =>
        // This will never happen, because it would mean openstack allows repeated ids.
        Sync[F].raiseError(UnexpectedStatus(BadRequest))
    }
  }
}