package pt.tecnico.dsi.openstack.keystone.models

import cats.{Applicative, derived}
import cats.derived.derived
import cats.derived.ShowPretty
import cats.instances.list.*
import cats.syntax.foldable.*
import io.circe.derivation.{ConfiguredCodec, ConfiguredEncoder}
import pt.tecnico.dsi.openstack.common.models.{Identifiable, Link}
import pt.tecnico.dsi.openstack.keystone.KeystoneClient

object Group:
  /**
   * Options to create a Group.
   * @param name The name of the group.
   * @param description The description of the group.
   * @param domainId The ID of the domain of the group. If the domain ID is not provided in the request, the Identity service will attempt
   *                 to pull the domain ID from the token used in the request. Note that this requires the use of a domain-scoped token.
   */
  case class Create(
    name: String,
    description: String = "",
    domainId: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty
  
  /**
   * Options to update a Group.
   * @param name The new name of the group.
   * @param description The new description of the group.
   */
  case class Update(
    name: Option[String] = None,
    description: Option[String] = None,
  ) derives ConfiguredEncoder, ShowPretty:
    lazy val needsUpdate: Boolean =
      // We could implement this with the next line, but that implementation is less reliable if the fields of this class change
      //  productIterator.asInstanceOf[Iterator[Option[Any]]].exists(_.isDefined)
      List(name, description).exists(_.isDefined)
final case class Group(
  id: String,
  name: String,
  description: String,
  domainId: String,
  links: List[Link] = List.empty,
) extends Identifiable derives ConfiguredCodec, ShowPretty { self =>
  def domain[F[_]](using client: KeystoneClient[F]): F[Domain] = client.domains(domainId)
  
  def users[F[_]](using client: KeystoneClient[F]): F[List[User]] = client.groups.listUsers(self.id)
  def addUser[F[_]](id: String)(using client: KeystoneClient[F]): F[Unit] = client.groups.addUser(self.id, id)
  def addUsers[F[_]: Applicative](ids: List[String])(using client: KeystoneClient[F]): F[Unit] = ids.traverse_(client.groups.addUser(self.id, _))
  def removeUser[F[_]](id: String)(using client: KeystoneClient[F]): F[Unit] = client.groups.removeUser(self.id, id)
  def removeUsers[F[_]: Applicative](ids: List[String])(using client: KeystoneClient[F]): F[Unit] = ids.traverse_(client.groups.removeUser(self.id, _))
  def isUserInGroup[F[_]](id: String)(using client: KeystoneClient[F]): F[Boolean] = client.groups.isUserInGroup(self.id, id)
}