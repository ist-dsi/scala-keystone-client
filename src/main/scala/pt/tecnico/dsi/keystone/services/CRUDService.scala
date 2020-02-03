package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.functor._
import io.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import org.http4s.{Header, Request, Uri}

abstract class CRUDService[F[_]: Sync, T: Codec]
  (
    baseUri: Uri,
    name: String,
    token: Header
  ) (implicit client: Client[F]) extends BaseService {

  protected val dsl = new Http4sClientDsl[F] with Methods
  import dsl._

  val plural = name + "s"

  /**
    * Takes a request and returns all the values returned.
    *
    */
  protected def withSeqWrapper(request: F[Request[F]]): F[Seq[T]] = {
    client.expect[Map[String, Seq[T]]](request)
      .map(map => map(plural))
  }

  /**
    * Takes a request and unwraps its return value.
    *
    * @param request
    * @return
    */
  protected def withWrapper(request: F[Request[F]]): F[T] = {
    client.expect[Map[String, T]](request)
      .map(map => map(name))
  }

  /**
    * Puts a value inside a wrapper.
    *
    * @param value
    * @return
    */
  protected def wrapBody(value: T): Map[String, T] = {
    Map(name -> value)
  }

  def get(id: String): F[T] = {
    withWrapper(
      GET(baseUri / id, token)
    )
  }

  def delete(id: String): F[Unit] = {
    client.expect(
      DELETE(baseUri / id, token)
    )
  }

  def create(value: T): F[T] = {
    withWrapper(
      POST(wrapBody(value), baseUri, token)
    )
  }

  def list(): F[Seq[T]] = {
    withSeqWrapper(
      GET(baseUri, token)
    )
  }

  def update(value: T): F[T] = {
    withWrapper(
      PATCH(wrapBody(value), baseUri, token)
    )
  }

}
