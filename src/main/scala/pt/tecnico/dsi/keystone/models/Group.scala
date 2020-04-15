package pt.tecnico.dsi.keystone.models

import cats.effect.Sync
import cats.instances.list._
import cats.syntax.foldable._
import fs2.Stream
import io.circe.Codec
import io.circe.derivation.{deriveCodec, renaming}
import pt.tecnico.dsi.keystone.KeystoneClient

object Group {
  implicit val codec: Codec.AsObject[Group] = deriveCodec(renaming.snakeCase, false, None)

  implicit class WithIdGroupExtensions[F[_]](group: WithId[Group])(implicit client: KeystoneClient[F], F: Sync[F]) {
    val users: Stream[F, WithId[User]] = client.groups.listUsers(group.id)
    def addUser(id: String): F[Unit] = client.groups.addUser(group.id, id)
    def addUsers(ids: List[String]): F[Unit] = ids.traverse_(client.groups.addUser(group.id, _))
    def removeUser(id: String): F[Unit] = client.groups.removeUser(group.id, id)
    def removeUsers(ids: List[String]): F[Unit] = ids.traverse_(client.groups.removeUser(group.id, _))
    def isUserInGroup(id: String): F[Boolean] = client.groups.isUserInGroup(group.id, id)
  }
}

case class Group(name: String, description: String, domainId: String) extends IdFetcher[Group] {
  override def getWithId[F[_]](implicit client: KeystoneClient[F]): F[WithId[Group]] = client.groups.get(name, domainId)

  def domain[F[_]](implicit client: KeystoneClient[F]): F[WithId[Domain]] = client.domains.get(domainId)

  def users[F[_]: Sync: KeystoneClient]: Stream[F, WithId[User]] = withId(_.users)
  def addUser[F[_]: Sync: KeystoneClient](id: String): F[Unit] = withId(_.addUser(id))
  def addUsers[F[_]: Sync: KeystoneClient](ids: List[String]): F[Unit] = withId(_.addUsers(ids))
  def removeUser[F[_]: Sync: KeystoneClient](id: String): F[Unit] = withId(_.removeUser(id))
  def removeUsers[F[_]: Sync: KeystoneClient](ids: List[String]): F[Unit] = withId(_.removeUsers(ids))
  def isUserInGroup[F[_]: Sync: KeystoneClient](id: String): F[Boolean] = withId(_.isUserInGroup(id))
}