package pt.tecnico.dsi.openstack.keystone.models

import java.time.OffsetDateTime
import cats.{Show, derived}
import cats.derived.ShowPretty
import cats.effect.Sync
import io.circe.{Decoder, HCursor}
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import io.chrisdavenport.cats.time.offsetdatetimeInstances

object Session {
  // Not implicit because otherwise the compiler interprets it as an implicit conversion
  def decoder(authToken: Header): Decoder[Session] = { cursor: HCursor =>
    val tokenCursor = cursor.downField("token")
    for {
      user <- tokenCursor.get[User]("user")
      expiresAt <- tokenCursor.get[OffsetDateTime]("expires_at")
      issuedAt <- tokenCursor.get[OffsetDateTime]("issued_at")
      auditIds <- tokenCursor.get[List[String]]("audit_ids")
      roles <- tokenCursor.getOrElse[List[Role]]("roles")(List.empty)
      catalog <- tokenCursor.getOrElse[List[CatalogEntry]]("catalog")(List.empty)
      // The sole reason for this handcrafted decoder, why is the scope directly at the root?
      // To make things more interesting obviously </sarcasm>
      scope <- tokenCursor.as[Scope]
    } yield Session(user, expiresAt, issuedAt, auditIds, roles, catalog, scope, authToken)
  }
  
  implicit val show: ShowPretty[Session] = {
    implicit val showHeader: Show[Header] = Show.show(h => s"${h.name}: <REDACTED>")
    derived.semiauto.showPretty
  }
}
final case class Session(
  user: User,
  expiresAt: OffsetDateTime,
  issuedAt: OffsetDateTime,
  auditIds: List[String],
  roles: List[Role] = List.empty,
  catalog: List[CatalogEntry] = List.empty,
  scope: Scope,
  authToken: Header,
) {
  /** @return the project id if the session is Project scoped. */
  def scopedProjectId: Option[String] = scope match {
    case Scope.Project(id, _, _) => Some(id)
    case _ => None
  }
  
  /** @return the domain id if the session is Domain scoped, otherwise the `defaultDomain`. */
  def scopedDomainId(defaultDomain: String = "default"): String = {
    // If the domain ID is not provided in the request, the Identity service will attempt to pull the domain ID
    // from the token used in the request. Note that this requires the use of a domain-scoped token.
    scope match {
      case Scope.Domain(id, _) => id
      case _ =>
        // https://github.com/openstack/keystone/blob/04316beecc0d20290fb36e7791eb3050953c1011/keystone/server/flask/common.py#L965
        // Not exactly the right value. Should be the default_domain_id from the keystone configuration, which we do not have access to.
        // According to the link above this logic will be removed. So we shouldn't include it, however if we do not
        // creating Users, Groups and Projects is no longer an idempotent operation :(
        defaultDomain
    }
  }
  
  /**
   * Builds a Openstack service client. Returns an Either because the catalog might not have the request service type,
   * region, or interface.
   * Example:
   * {{{
   *   val neutron: Either[String, NeutronClient[IO]] = keystone.session.clientBuilder(NeutronClient, "RegionA")
   * }}}
   * @param builder a builder for a specific Openstack client
   * @param region the region for which to get the service url from the catalog
   * @param interface the interface for which to get the service url from the catalog
   */
  def clientBuilder[F[_]: Sync: Client](builder: ClientBuilder, region: String,
    interface: Interface = Interface.Public): Either[String, builder.OpenstackClient[F]] = for {
    entry <- catalog.find(_.`type` == builder.`type`).toRight(s"""Could not find "${builder.`type`}" service in the catalog""")
    publicUrls <- entry.urlsOf(interface).toRight(s"""Service "${builder.`type`}" does not have $interface endpoints""")
    regionUrl <- publicUrls.get(region).toRight(s"""Service "${builder.`type`}" does not have endpoints for region $region""")
    // Openstack tries to be clever and for some services appends the project id to the **public** url
    // The clients are expecting a clean uri, since they can be used for administrative purposes
    strippedUrlString = scopedProjectId.map(projectId => regionUrl.stripSuffix(s"/$projectId")).getOrElse(regionUrl)
    uri <- Uri.fromString(strippedUrlString).left.map(_.message)
  } yield builder(uri, this)
}

trait ClientBuilder {
  /** The concrete Openstack client type this `ClientBuilder` will create. */
  type OpenstackClient[F[_]]
  /** The catalog entry `type` for the `OpenstackClient`. Eg: for the neutron client it would be "network" */
  val `type`: String
  
  def apply[F[_]: Sync: Client](baseUri: Uri, session: Session): OpenstackClient[F]
}
