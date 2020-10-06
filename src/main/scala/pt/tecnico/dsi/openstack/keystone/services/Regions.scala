package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{KeystoneError, Region, Session}

final class Regions[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Region, Region.Create, Region.Update](baseUri, "region", session.authToken) {
  /**
    * @param parentRegionId filters the response by a parent region, by ID.
    * @return a stream of regions filtered by the various parameters.
    */
  def list(parentRegionId: Option[String] = None): F[List[Region]] =
    list(Query.fromVector(Vector(
      "parent_region_id" -> parentRegionId,
    )))
  
  override def update(id: String, update: Region.Update, extraHeaders: Header*): F[Region] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override def defaultResolveConflict(existing: Region, create: Region.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[Region] = {
    val updated = Region.Update(
      Option(create.description).filter(_ != existing.description),
      if (create.parentRegionId != existing.parentRegionId) create.parentRegionId else None,
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Sync[F].pure(existing)
  }
  override def createOrUpdate(create: Region.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (Region, Region.Create) => F[Region] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Region] = {
    val conflicting = """.*?Duplicate ID, ([^.]+)\..*?""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders) {
      case KeystoneError(conflicting(id), Conflict.code, _) => apply(id).flatMap(resolveConflict(_, create))
    }
  }
}