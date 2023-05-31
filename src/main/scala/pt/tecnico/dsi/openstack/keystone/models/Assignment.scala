package pt.tecnico.dsi.openstack.keystone.models

import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.ConfiguredEncoder
import io.circe.{Decoder, Encoder, HCursor}
import pt.tecnico.dsi.openstack.common.models.Link
import pt.tecnico.dsi.openstack.keystone.KeystoneClient

object Assignment:
  given Decoder[Assignment] = (cursor: HCursor) => for
    roleId <- cursor.downField("role").get[String]("id")
    subjectType = if cursor.downField("user").succeeded then "user" else "group"
    subjectId <- cursor.downField(subjectType).get[String]("id")
    scope <- cursor.get[Scope]("scope")
    links <- cursor.get[List[Link]]("links")(Link.linksDecoder)
  yield subjectType match
    case "user" => UserAssignment(roleId, subjectId, scope, links)
    case "group" => GroupAssignment(roleId, subjectId, scope, links)
sealed trait Assignment derives ShowPretty:
  def roleId: String
  def subjectId: String
  // The returned scope will have nulls in every field apart from id (unless include_names was specified). The unscoped will never happen
  def scope: Scope
  def links: List[Link]
  
  def role[F[_]](using client: KeystoneClient[F]): F[Role] = client.roles.apply(roleId)

object UserAssignment:
  given Decoder[UserAssignment] = Assignment.given_Decoder_Assignment.asInstanceOf[Decoder[UserAssignment]]
case class UserAssignment(
  roleId: String,
  userId: String,
  scope: Scope,
  links: List[Link],
) extends Assignment derives ConfiguredEncoder, ShowPretty:
  override def subjectId: String = userId
  
  def user[F[_]](using client: KeystoneClient[F]): F[User] = client.users.apply(userId)

object GroupAssignment:
  given Decoder[GroupAssignment] = Assignment.given_Decoder_Assignment.asInstanceOf[Decoder[GroupAssignment]]
case class GroupAssignment(
  roleId: String,
  groupId: String,
  scope: Scope,
  links: List[Link],
) extends Assignment derives ConfiguredEncoder, ShowPretty{
  override def subjectId: String = groupId
  
  def group[F[_]](using client: KeystoneClient[F]): F[Group] = client.groups.apply(groupId)
}