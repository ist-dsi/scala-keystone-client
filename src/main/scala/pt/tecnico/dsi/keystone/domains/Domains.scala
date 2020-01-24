package pt.tecnico.dsi.keystone.domains

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.domains.models.{Domain, DomainWrapper, ListResponse}

class Domains[F[_]: Sync](uri: Uri, token: String)(implicit client: Client[F]) { self =>

  private val authHeader = Header("X-Auth-Token", token)
  private val request: Request[F] = Request().withHeaders(Headers.of(authHeader))

  /**
    * List all domains.
    */
  def list : F[ListResponse] = {
    val request = self.request.withUri(uri).withMethod(Method.GET)
    client.expect(request)
  }

  /**
    * Show domain details.
    */
  def show(domainId: String) : F[Domain] = {
    val request = self.request.withUri(uri / domainId).withMethod(Method.GET)
    client.expect(request)
  }

  /**
    * Deletes a certain domain.
    */
  def delete(domainId: String): F[Unit] = {
    val request = self.request.withUri(uri / domainId).withMethod(Method.DELETE)
    client.expect(request)
  }

  /**
    * Create a domain.
    */
  def create(domainWrapper: DomainWrapper): F[DomainWrapper] = {
    val request = self.request.withUri(uri).withMethod(Method.POST).withEntity(domainWrapper)
    client.expect(request)
  }

  /**
    * Update domain.
    */
  def update(domainWrapper: DomainWrapper): F[DomainWrapper] = {
    val request = self.request.withUri(uri).withMethod(Method.PATCH).withEntity(domainWrapper)
    client.expect(request)
  }

}
