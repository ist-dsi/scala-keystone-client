package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.{Chunk, Stream}
import io.circe._
import org.http4s.Status.Successful
import org.http4s.circe.decodeUri
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import org.http4s.{Header, Query, Request, Response, Uri}
import org.http4s.Status.Conflict
import pt.tecnico.dsi.keystone.models.WithId

abstract class CRUDService[F[_], T: Encoder: Decoder]
  (baseUri: Uri, val name: String, subjectToken: Header)(implicit client: Client[F], F: Sync[F]) {

  protected val dsl = new Http4sClientDsl[F] with Methods
  import dsl._

  protected val plural = s"${name}s"
  val uri: Uri = baseUri / plural

  /** Takes a request and unwraps its return value. */
  protected def unwrap(request: F[Request[F]]): F[WithId[T]] = client.expect[Map[String, WithId[T]]](request).map(_.apply(name))
  /** Puts a value inside a wrapper. */
  protected def wrap(value: T): Map[String, T] = Map(name -> value)

  implicit val paginatedDecoder: Decoder[(Option[Uri], List[WithId[T]])] = (c: HCursor) => for {
    links <- c.downField("links").get[Option[Uri]]("next")
    domainObjectList <- c.downField(plural).as[List[WithId[T]]]
  } yield (links, domainObjectList)

  def list(query: Query = Query.empty): Stream[F, WithId[T]] =
    Stream.unfoldChunkEval[F, Option[Uri], WithId[T]](Some(uri)) {
      case Some(uri) =>
        for {
          request <- GET(uri.copy(query = uri.query ++ query.pairs), subjectToken)
          (next, entries) <- client.expect[(Option[Uri], List[WithId[T]])](request)
        } yield Some((Chunk.iterable(entries), next))
      case None => Sync[F].pure(None)
    }

  def create(value: T): F[WithId[T]] = unwrap(POST(wrap(value), uri, subjectToken))

  protected def createHandleConflict(value: T)(onConflict: F[WithId[T]]): F[WithId[T]] =
    client.fetch(POST(wrap(value), uri, subjectToken)){
      case Successful(response: Response[F]) => response.as[Map[String, WithId[T]]].map(map => map(name))
      case Conflict(_) => onConflict
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  protected def deleteHandleConflict(id: String)(onConflict: F[Unit]): F[Unit] =
    client.fetch(DELETE(uri / id, subjectToken)) {
      case Successful(_) => F.pure(())
      case Conflict(_) => onConflict
      case response => F.raiseError(UnexpectedStatus(response.status))
    }

  def get(id: String): F[WithId[T]] = unwrap(GET(uri / id, subjectToken))

  def update(value: WithId[T]): F[WithId[T]] = update(value.id, value.model)
  def update(id: String, value: T): F[WithId[T]] = unwrap(PATCH(wrap(value), uri / id, subjectToken))

  def delete(id: String): F[Unit] = deleteHandleConflict(id) {
    // Error means it was already deleted?
    F.pure(())
  }

  /*{
    "error" : {
      "message" : "Conflict occurred attempting to store user - Duplicate entry found with name teste at domain ID default.",
      "code" : 409,
      "title" : "Conflict"
    }
  }*/
}