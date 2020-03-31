package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import fs2.Stream
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.keystone.models.{CatalogEntry, Domain, Project, Scope, Session, WithId}

class Authentication[F[_]](baseUri: Uri, authToken: Header)(implicit client: Client[F], F: Sync[F]) {
  protected val genericListEndpoint: GenericListEndpoint[F] = ListEndpoint[F](authToken)

  protected val dsl = new Http4sClientDsl[F] with Methods
  import dsl._

  val uri: Uri = baseUri / "auth"

  private def subjectToken(token: String): Header = Header("X-Subject-Token", token)

  /** Validates and shows information for `token`, including its expiration date and authorization scope. */
  def validateAndShowInformation(token: String): F[Session] = client.expect(GET(uri / "tokens", authToken, subjectToken(token)))

  /** Validates `token`. Similar to `validateAndShowInformation` but no body is returned. */
  def validate(token: String): F[Boolean] = client.successful(HEAD(uri / "tokens", authToken, subjectToken(token)))

  /** Revoke `token` which is immediately not valid, regardless of the expiresAt attribute value. */
  def revoke(token: String): F[Unit] = client.expect(DELETE(uri / "tokens", authToken, subjectToken(token)))

  /** Get service catalog. */
  def serviceCatalog: F[List[CatalogEntry]] = genericListEndpoint[CatalogEntry]("catalog", uri / "catalog").compile.toList

  /** Get available project scopes. */
  def projectScopes: Stream[F, WithId[Project]] = genericListEndpoint[WithId[Project]]("projects", uri / "projects")
  /** Get available domain scopes */
  def domainScopes: Stream[F, WithId[Domain]] = genericListEndpoint[WithId[Domain]]("domains", uri / "domains")
  /** Get available system scopes */
  def systemScopes: F[Scope.System] = client.expect(GET(uri / "system"))
}