package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.Method.{DELETE, GET, HEAD}
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.Service
import pt.tecnico.dsi.openstack.keystone.models.Scope.System
import pt.tecnico.dsi.openstack.keystone.models.{CatalogEntry, Domain, Project, Session}

final class Authentication[F[_]: Sync: Client](baseUri: Uri, session: Session) extends Service[F](session.authToken) {
  import dsl._

  val uri: Uri = baseUri / "auth"

  private def subjectToken(token: String): Header = Header("X-Subject-Token", token)

  /** Validates and shows information for `token`, including its expiration date and authorization scope. */
  def validateAndShowInformation(token: String): F[Session] =
    client.expect(GET(uri / "tokens", authToken, subjectToken(token)))(jsonDecoder(Session.decoder(authToken)))

  /** Validates `token`. Similar to `validateAndShowInformation` but no body is returned. */
  def validate(token: String): F[Boolean] = client.successful(HEAD(uri / "tokens", authToken, subjectToken(token)))

  /** Revoke `token` which is immediately not valid, regardless of the expiresAt attribute value. */
  def revoke(token: String): F[Unit] = client.expect(DELETE(uri / "tokens", authToken, subjectToken(token)))

  /** Get service catalog. */
  def serviceCatalog: F[List[CatalogEntry]] = super.list[CatalogEntry]("catalog", uri / "catalog", Query.empty).compile.toList

  /** Get available project scopes. */
  def projectScopes: Stream[F, Project] = super.list[Project]("projects", uri / "projects", Query.empty)
  /** Get available domain scopes */
  def domainScopes: Stream[F, Domain] = super.list[Domain]("domains", uri / "domains", Query.empty)
  /** Get available system scopes */
  def systemScopes: F[System] = client.expect(GET(uri / "system"))
}