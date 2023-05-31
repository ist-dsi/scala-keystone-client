package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Concurrent
import cats.syntax.flatMap.*
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{KeystoneError, Region, Session}

/**
 * The service class for regions.
 * @define domainModel region
 */
final class Regions[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Region, Region.Create, Region.Update](baseUri, "region", session.authToken) {
  
  /**
    * @param parentRegionId filters the response by a parent region, by ID.
    * @return a stream of regions filtered by the various parameters.
    */
  def list(parentRegionId: Option[String] = None): F[List[Region]] = list(Query("parent_region_id" -> parentRegionId))
  
  override def defaultResolveConflict(existing: Region, create: Region.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[Region] =
    val updated = Region.Update(
      Option(create.description).filter(_ != existing.description),
      if create.parentRegionId != existing.parentRegionId then create.parentRegionId else None,
    )
    if updated.needsUpdate then update(existing.id, updated, extraHeaders*)
    else Concurrent[F].pure(existing)
  override def createOrUpdate(create: Region.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (Region, Region.Create) => F[Region] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Region] =
    val conflicting = """.*?Duplicate ID, ([^.]+)\..*?""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders):
      case KeystoneError(conflicting(id), Conflict.code, _) =>
        apply(id).flatMap { existing =>
          getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct name.")
          resolveConflict(existing, create)
        }
  
  override def update(id: String, update: Region.Update, extraHeaders: Header.ToRaw*): F[Region] =
    super.patch(wrappedAt, update, uri / id, extraHeaders*)
}