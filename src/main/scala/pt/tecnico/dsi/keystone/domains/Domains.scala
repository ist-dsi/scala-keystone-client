package pt.tecnico.dsi.keystone.domains

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import pt.tecnico.dsi.keystone.domains.models.{Domain, DomainWrapper, ListResponse}

class Domains[F[_]: Sync](uri: Uri, token: Header)(implicit client: Client[F]) {
  private val dsl = new Http4sClientDsl[F] with Methods {}
  import dsl._

  /**
    * List all domains.
    */
  def list : F[ListResponse] = client.expect(GET(uri, token))

  /**
    * Show domain details.
    */
  def show(domainId: String) : F[Domain] = client.expect(GET.apply(uri / domainId))

  /**
    * Deletes a certain domain.
    */
  def delete(domainId: String): F[Unit] = client.expect(DELETE.apply(uri / domainId, token))

  /**
    * Create a domain.
    */
  def create(domainWrapper: DomainWrapper): F[DomainWrapper] = client.expect(POST.apply(domainWrapper, uri, token))

  /**
    * Update domain.
    */
  def update(domainWrapper: DomainWrapper): F[DomainWrapper] = client.expect(PATCH.apply(domainWrapper, uri, token))
}
