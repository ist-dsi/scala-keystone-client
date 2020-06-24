package pt.tecnico.dsi.keystone.services

import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Encoder
import pt.tecnico.dsi.keystone.models.Enabler

trait EnableDisableEndpoints[F[_], T <: Enabler[T]] { self: CrudService[F, T] =>

  def disable(id: String)(implicit encoder: Encoder[T]): F[Unit] = updateEnable(id, value = false)
  def enable(id: String)(implicit encoder: Encoder[T]): F[Unit] = updateEnable(id, value = true)

  private def updateEnable(id: String, value: Boolean)(implicit encoder: Encoder[T]) : F[Unit] = {
    for {
      obj <- get(id)
      _ <- update(obj.id, obj.withEnabled(value))
    } yield ()
  }
}
