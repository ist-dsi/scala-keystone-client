package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import io.circe.Codec
import org.http4s.Status.{Conflict, NotFound, Successful}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dsl.impl.Methods
import org.http4s.{Header, Query, Request, Response, Uri}
import pt.tecnico.dsi.keystone.models.{WithEnabled, WithId}

abstract class CRUDService[F[_], T: Codec](val baseUri: Uri, val name: String, val authToken: Header)
                                          (implicit val client: Client[F], protected val F: Sync[F]) {
  protected val dsl = new Http4sClientDsl[F] with Methods
  import dsl._

  val pluralName = s"${name}s"
  val uri: Uri = baseUri / pluralName

  /** Takes a request and unwraps its return value. */
  protected def unwrap(request: F[Request[F]]): F[WithId[T]] = client.expect[Map[String, WithId[T]]](request).map(_.apply(name))
  /** Puts a value inside a wrapper. */
  protected def wrap(value: T): Map[String, T] = Map(name -> value)

  protected val genericListEndpoint: GenericListEndpoint[F] = ListEndpoint[F](authToken)
  def list(): Stream[F, WithId[T]] = genericListEndpoint[WithId[T]](pluralName, uri, Query.empty)
  def list(query: Query): Stream[F, WithId[T]] = genericListEndpoint[WithId[T]](pluralName, uri, query)

  def create(value: T): F[WithId[T]] = unwrap(POST(wrap(value), uri, authToken))

  protected def createHandleConflict(value: T)(onConflict: Response[F] => F[WithId[T]]): F[WithId[T]] =
    client.fetch(POST(wrap(value), uri, authToken)){
      case Successful(response) => response.as[Map[String, WithId[T]]].map(map => map(name))
      case Conflict(response) => onConflict(response)
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  def get(id: String): F[WithId[T]] = unwrap(GET(uri / id, authToken))

  def update(value: WithId[T]): F[WithId[T]] = update(value.id, value.model)
  def update(id: String, value: T): F[WithId[T]] = unwrap(PATCH(wrap(value), uri / id, authToken))

  def delete(value: WithId[T]): F[Unit] = delete(value.id)
  def delete(id: String): F[Unit] =
    client.fetch(DELETE(uri / id, authToken)) {
      case Successful(_) | NotFound(_) => F.pure(())
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  def disable(id: String)(implicit ev: T <:< WithEnabled[T]): F[Unit] = updateEnable(id, value = false)
  def enable(id: String)(implicit ev: T <:< WithEnabled[T]): F[Unit] = updateEnable(id, value = true)

  private def updateEnable(id: String, value: Boolean)(implicit ev: T <:< WithEnabled[T]) : F[Unit] = {
    for {
      obj <- get(id)
      _ <- update(obj.id, ev(obj.model).withEnabled(value))
    } yield ()
  }
}