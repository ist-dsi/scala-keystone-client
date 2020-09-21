package pt.tecnico.dsi.openstack.keystone.models

import java.time.OffsetDateTime
import io.circe.{Decoder, HCursor}

object Session {
  implicit val decoder: Decoder[Session] = { cursor: HCursor =>
    val tokenCursor = cursor.downField("token")
    for {
      user <- tokenCursor.get[User]("user")
      expiresAt <- tokenCursor.get[OffsetDateTime]("expires_at")
      issuedAt <- tokenCursor.get[OffsetDateTime]("issued_at")
      auditIds <- tokenCursor.get[List[String]]("audit_ids")
      roles <- tokenCursor.getOrElse[List[Role]]("roles")(List.empty)
      catalog <- tokenCursor.getOrElse[List[CatalogEntry]]("catalog")(List.empty)
      scope <- tokenCursor.as[Scope] // The sole reason for this handcrafted decoder
    } yield Session(user, expiresAt, issuedAt, auditIds, roles, catalog, scope)
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
) {
  // For a given type (compute, network, etc) the catalog only has one CatalogEntry so we use .head to "drop the list"
  lazy val catalogPerType: Map[String, CatalogEntry] = catalog.groupBy(_.`type`).view.mapValues(_.head).toMap
  
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
  
  def urlOf(`type`: String, region: String): Option[String] = catalog.collectFirst {
    case entry @ CatalogEntry(`type`, _, _, _) => entry.urlOf(Interface.Public, region)
  }.flatten
}