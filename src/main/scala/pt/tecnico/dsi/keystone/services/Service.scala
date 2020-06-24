package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.{Chunk, Stream}
import io.circe.{Decoder, Encoder, HCursor, Json, Printer}
import org.http4s.Status.{Gone, NotFound, Successful}
import org.http4s.circe.decodeUri
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.Method.{DELETE, GET, PATCH, POST}
import org.http4s.{EntityDecoder, EntityEncoder, Header, Query, Request, Uri, circe}

abstract class Service[F[_]](protected val authToken: Header)(implicit protected val client: Client[F], protected val F: Sync[F]) {
  protected val dsl = new Http4sClientDsl[F] {}
  import dsl._

  private val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf(jsonPrinter)
  implicit def jsonDecoder[A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf

  // Without this decoding to Unit wont work. This makes the EntityDecoder[F, Unit] defined in EntityDecoder companion object
  // have a higher priority than the jsonDecoder defined above. https://github.com/http4s/http4s/issues/2806
  implicit val void: EntityDecoder[F, Unit] = EntityDecoder.void

  protected def unwrapped[R](at: Option[String] = None)(implicit decoder: Decoder[R]): EntityDecoder[F, R] =
    jsonDecoder(at.fold(decoder)(decoder.at))
  protected def wrapped[R](at: Option[String] = None)(implicit encoder: Encoder[R]): EntityEncoder[F, R] =
    jsonEncoder(at.fold(encoder) { name =>
      encoder.mapJson(originalJson => Json.obj(name -> originalJson))
    })

  protected def expectUnwrapped[R: Decoder](request: F[Request[F]], wrappedAt: Option[String]): F[R] =
    client.expect(request)(unwrapped(wrappedAt))

  protected def get[R: Decoder](uri: Uri, wrappedAt: Option[String]): F[R] =
    expectUnwrapped(GET(uri, authToken), wrappedAt)

  protected def update[V: Encoder, R: Decoder](uri: Uri, value: V, wrappedAt: Option[String]): F[R] = {
    implicit val e: EntityEncoder[F, V] = wrapped(wrappedAt)
    expectUnwrapped(PATCH(value, uri, authToken), wrappedAt)
  }

  protected def create[V: Encoder, R: Decoder](uri: Uri, value: V, wrappedAt: Option[String]): F[R] = {
    implicit val e: EntityEncoder[F, V] = wrapped(wrappedAt)
    expectUnwrapped(POST(value, uri, authToken), wrappedAt)
  }

  protected def list[R: Decoder](baseKey: String, uri: Uri, query: Query): Stream[F, R] = {
    implicit val paginatedDecoder: Decoder[(Option[Uri], List[R])] = (c: HCursor) => for {
      links <- c.downField("links").get[Option[Uri]]("next")
      objectList <- c.downField(baseKey).as[List[R]]
    } yield (links, objectList)

    Stream.unfoldChunkEval[F, Option[Uri], R](Some(uri)) {
      case Some(uri) =>
        for {
          // The new uri query params must have precedence, otherwise we would always be getting the same page/marker/offset
          request <- GET(uri.copy(query = query ++ uri.query.pairs), authToken)
          (next, entries) <- client.expect[(Option[Uri], List[R])](request)
        } yield Some((Chunk.iterable(entries), next))
      case None => F.pure(None)
    }
  }

  protected def delete(uri: Uri): F[Unit] =
    client.fetch(DELETE(uri, authToken)) {
      case Successful(_) | NotFound(_) | Gone(_) => F.pure(())
      case response => F.raiseError(UnexpectedStatus(response.status))
    }
}