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
      roles <- tokenCursor.getOrElse[List[Role]]("roles")(List.empty)
      auditIds <- tokenCursor.get[List[String]]("audit_ids")
      catalog <- tokenCursor.getOrElse[List[CatalogEntry]]("catalog")(List.empty)
      scope <- tokenCursor.as[Scope]
    } yield Session(user, expiresAt, issuedAt, auditIds, roles, catalog, scope)
  }
}
final case class Session(
  user: User,
  expiredAt: OffsetDateTime,
  issuedAt: OffsetDateTime,
  auditIds: List[String],
  roles: List[Role],
  catalog: List[CatalogEntry],
  scope: Scope,
) {
  // For a given type (compute, network, etc) the catalog only has one CatalogEntry so we use .head to "drop the list"
  lazy val catalogPerType: Map[String, CatalogEntry] = catalog.groupBy(_.`type`).view.mapValues(_.head).toMap
}