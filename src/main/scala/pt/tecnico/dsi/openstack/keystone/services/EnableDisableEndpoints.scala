package pt.tecnico.dsi.openstack.keystone.services

import cats.syntax.flatMap._
import cats.syntax.functor._
import pt.tecnico.dsi.openstack.keystone.models.Enabler

trait EnableDisableEndpoints[F[_], T <: Enabler[T]] { self: CrudService[F, T] =>

  def disable(id: String): F[Unit] = updateEnable(id, value = false)
  def enable(id: String): F[Unit] = updateEnable(id, value = true)

  private def updateEnable(id: String, value: Boolean) : F[Unit] = {
    for {
      obj <- get(id)
      _ <- update(obj.id, obj.withEnabled(value))
    } yield ()
  }
}
