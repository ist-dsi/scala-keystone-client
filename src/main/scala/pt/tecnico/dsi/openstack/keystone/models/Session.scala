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
case class Session(
  user: User,
  expiredAt: OffsetDateTime,
  issuedAt: OffsetDateTime,
  auditIds: List[String],
  roles: List[Role],
  catalog: List[CatalogEntry],
  scope: Scope,
)