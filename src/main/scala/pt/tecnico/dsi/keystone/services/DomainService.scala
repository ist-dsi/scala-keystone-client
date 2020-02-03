package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.functor._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import pt.tecnico.dsi.keystone.models.domains.{Domain, DomainWrapper, Domains}

import pt.tecnico.dsi.keystone.services.BaseService

class DomainService[F[_]: Sync](uri: Uri, token: Header)
                               (implicit client: Client[F]) extends BaseService {

  private val dsl = new Http4sClientDsl[F] with Methods {}
  import dsl._

  /**
    * Creates a new domain
    * @param domain The Domain to create
    * @return the new domain
    */
  def create(domain: Domain): F[Domain] = {
    val wrapper = DomainWrapper(domain)
    client.expect[DomainWrapper](
      POST(wrapper, uri, token)
    ).map(_.domain)
  }

  /**
    * Creates a new domain
    *
    * @param name        the name of the new domain
    * @param description the description of the new domain
    * @param enabled     the enabled status of the new domain
    * @return the new domain
    */
  def create(name: String, description: String, enabled: Boolean): F[Domain] = {
    create(Domain(
      name = name,
      description = description,
      enabled = enabled
    ))
  }

  /**
    * Updates an existing domain
    *
    * @param domain the domain set to update
    * @return the updated domain
    */
  def update(domain: Domain): F[Domain] = {
    val wrapper = DomainWrapper(domain)
    client.expect[DomainWrapper](
      PATCH(wrapper, uri, token)
    ).map(_.domain)
  }

  /**
    * Get detailed information on a domain by id
    *
    * @param domainId the domain identifier
    * @return the domain
    */
  def get(domainId: String): F[Domain] = {
    client.expect[DomainWrapper](
      GET(uri / domainId)
    ).map(_.domain)
  }

  /**
    * Get detailed information on a domain by
    *
    * @param domainName the domain name
    * @return the domain
    */
  def getByName(domainName: String): F[Seq[Domain]] = {
    client.expect[Domains](
      GET(uri.withQueryParam("name", domainName), token)
    ).map(_.domains)
  }

  /**
    * Deletes a domain by id.
    *
    * @param domainId the domain id
    */
  def delete(domainId: String): F[Unit] = {
    client.expect(DELETE(uri / domainId, token))
  }

  /**
    * Lists all domains the current token has access to.
    *
    * @return list of domains
    */
  def list(): F[Seq[Domain]] = {
    client.expect[Domains](
      GET(uri, token)
    ).map(_.domains)
  }

}
