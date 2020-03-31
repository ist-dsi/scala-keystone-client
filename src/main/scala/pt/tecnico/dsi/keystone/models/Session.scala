package pt.tecnico.dsi.keystone.models

import java.time.OffsetDateTime
import io.circe.{Decoder, HCursor}
import pt.tecnico.dsi.keystone.models.Scope.Unscoped

object Session {
  implicit val decoder: Decoder[Session] = { cursor: HCursor =>
    val tokenCursor = cursor.downField("token")
    for {
      user <- tokenCursor.get[WithId[User]]("user")
      expiresAt <- tokenCursor.get[OffsetDateTime]("expires_at")
      issuedAt <- tokenCursor.get[OffsetDateTime]("issued_at")
      //roles <- tokenCursor.get[Option[List[Role]]]("roles")
      auditIds <- tokenCursor.get[List[String]]("audit_ids")
      catalog <- tokenCursor.getOrElse[List[CatalogEntry]]("catalog")(List.empty)
      scope <- tokenCursor.as[Scope]
    } yield scope match {
      case Unscoped => UnscopedSession(user, expiresAt, issuedAt, auditIds)
      case _ => ScopedSession(user, expiresAt, issuedAt/*, roles.getOrElse(List.empty)*/, catalog, scope, auditIds)
    }
  }
}
sealed trait Session {
  def user: WithId[User]
  def expiredAt: OffsetDateTime
  def issuedAt: OffsetDateTime
  def auditIds: List[String]
}

case class ScopedSession(
  user: WithId[User],
  expiredAt: OffsetDateTime,
  issuedAt: OffsetDateTime,
//  roles: List[Role],
  catalog: List[CatalogEntry],
  scope: Scope,
  auditIds: List[String]
) extends Session

case class UnscopedSession(
  user: WithId[User],
  expiredAt: OffsetDateTime,
  issuedAt: OffsetDateTime,
  auditIds: List[String]
) extends Session
