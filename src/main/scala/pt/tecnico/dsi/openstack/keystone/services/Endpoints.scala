package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Concurrent
import cats.syntax.flatMap._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Endpoint, Interface, KeystoneError, Session}

final class Endpoints[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Endpoint, Endpoint.Create, Endpoint.Update](baseUri, "endpoint", session.authToken)
    with EnableDisableEndpoints[F, Endpoint] {
  
  /**
    * @param interface filters the response by an interface.
    * @param serviceId filters the response by a domain ID.
    * @return a stream of endpoints filtered by the various parameters.
    */
  def list(interface: Option[Interface] = None, serviceId: Option[String] = None, regionId: Option[String] = None): F[List[Endpoint]] =
    list(Query(
      "interface" -> interface.map(_.toString.toLowerCase),
      "service_id" -> serviceId,
      "region_id" -> regionId,
    ))
  
  override def defaultResolveConflict(existing: Endpoint, create: Endpoint.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[Endpoint] = {
    val updated = Endpoint.Update(
      url = Option(create.url).filter(_ != existing.url),
      enabled = Option(create.enabled).filter(_ != existing.enabled),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Concurrent[F].pure(existing)
  }
  override def createOrUpdate(create: Endpoint.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (Endpoint, Endpoint.Create) => F[Endpoint] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Endpoint] = {
    // Openstack always creates a new endpoint now matter what.
    list(Some(create.interface), Some(create.serviceId), Some(create.regionId)).flatMap {
      case Nil => this.create(create, extraHeaders:_*)
      case List(existing) =>
        getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct interface, serviceId, and regionId.")
        resolveConflict(existing, create)
      case _ =>
        val message = s"""Cannot create a Endpoint idempotently because more than one exists with
                         |Interface: ${create.interface}
                         |ServiceId: ${create.serviceId}
                         |RegionId ${create.regionId}""".stripMargin
        Concurrent[F].raiseError(KeystoneError(message, Conflict.code, Conflict.reason))
    }
  }
  
  override def update(id: String, update: Endpoint.Update, extraHeaders: Header.ToRaw*): F[Endpoint] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override protected def updateEnable(id: String, enabled: Boolean): F[Endpoint] =
    update(id, Endpoint.Update(enabled = Some(enabled)))
}
