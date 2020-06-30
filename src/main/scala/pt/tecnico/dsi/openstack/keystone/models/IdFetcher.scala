package pt.tecnico.dsi.openstack.keystone.models

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.common.models.WithId

trait IdFetcher[T <: IdFetcher[T]] {
  def getWithId[F[_]: Sync](implicit client: KeystoneClient[F]): F[WithId[T]]

  def withId[F[_]: Sync: KeystoneClient, R](f: WithId[T] => F[R]): F[R] = getWithId.flatMap(f)
  def withId[F[_]: Sync: KeystoneClient, R](f: WithId[T] => Stream[F, R]): Stream[F, R] = Stream.eval(getWithId).flatMap(f)
}