package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Concurrent
import cats.syntax.flatMap._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{KeystoneError, Service, Session}

/**
 * The service class for services.
 * @define domainModel service
 */
final class Services[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Service, Service.Create, Service.Update](baseUri, "service", session.authToken)
    with EnableDisableEndpoints[F, Service] {
  
  /**
    * @param name Filters the response by a service name.
    * @param `type` Filters the response by a service type.
    * @return a stream of services filtered by the various parameters.
    */
  def list(name: Option[String] = None, `type`: Option[String] = None): F[List[Service]] = list(Query("name" -> name, "type" -> `type`))
  
  override def defaultResolveConflict(existing: Service, create: Service.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[Service] = {
    val updated = Service.Update(
      description = if (create.description != existing.description) create.description else None,
      enabled = Option(create.enabled).filter(_ != existing.enabled),
    )
    if (updated.needsUpdate) update(existing.id, updated, extraHeaders:_*)
    else Concurrent[F].pure(existing)
  }
  override def createOrUpdate(create: Service.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (Service, Service.Create) => F[Service] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Service] = {
    // Openstack always creates a new service now matter what.
    list(Some(create.name), Some(create.`type`)).flatMap {
      case Nil => this.create(create, extraHeaders:_*)
      case List(existing) =>
        getLogger.info(s"createOrUpdate: found unique $name (id: ${existing.id}) with the correct name and type")
        resolveConflict(existing, create)
      case _ =>
        val message = s"Cannot create a service idempotently because more than one service with name: ${create.name} and type: ${create.`type`} exists."
        Concurrent[F].raiseError(KeystoneError(message, Conflict.code, Conflict.reason))
    }
  }
  
  override def update(id: String, update: Service.Update, extraHeaders: Header.ToRaw*): F[Service] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override protected def updateEnable(id: String, enabled: Boolean): F[Service] =
    update(id, Service.Update(enabled = Some(enabled)))
}