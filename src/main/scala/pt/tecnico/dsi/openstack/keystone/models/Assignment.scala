package pt.tecnico.dsi.openstack.keystone.models

import io.circe.{Decoder, HCursor}
import pt.tecnico.dsi.openstack.common.models.Link
import pt.tecnico.dsi.openstack.keystone.KeystoneClient

object Assignment {
  implicit val decoder: Decoder[Assignment] = (cursor: HCursor) => for {
    roleId <- cursor.downField("role").get[String]("id")
    subjectType = if (cursor.downField("user").succeeded) "user" else "group"
    subjectId <- cursor.downField(subjectType).get[String]("id")
    scope <- cursor.get[Scope]("scope")
    links <- cursor.get[List[Link]]("links")(Link.linksDecoder)
  } yield subjectType match {
    case "user" => UserAssignment(roleId, subjectId, scope, links)
    case "group" => GroupAssignment(roleId, subjectId, scope, links)
  }
}
sealed trait Assignment {
  def roleId: String
  def subjectId: String
  // The returned scope will have nulls in every field apart from id (unless include_names was specified). The unscoped will never happen
  def scope: Scope
  def links: List[Link]
  
  def role[F[_]](implicit client: KeystoneClient[F]): F[Role] = client.roles.apply(roleId)
}

object UserAssignment {
  implicit val decoder: Decoder[UserAssignment] = Assignment.decoder.asInstanceOf[Decoder[UserAssignment]]
}
case class UserAssignment(
  roleId: String,
  userId: String,
  scope: Scope,
  links: List[Link],
) extends Assignment {
  override def subjectId: String = userId
  
  def user[F[_]](implicit client: KeystoneClient[F]): F[User] = client.users.apply(userId)
}

object GroupAssignment {
  implicit val decoder: Decoder[GroupAssignment] = Assignment.decoder.asInstanceOf[Decoder[GroupAssignment]]
}
case class GroupAssignment(
  roleId: String,
  groupId: String,
  scope: Scope,
  links: List[Link],
) extends Assignment {
  override def subjectId: String = groupId
  
  def group[F[_]](implicit client: KeystoneClient[F]): F[Group] = client.groups.apply(groupId)
}