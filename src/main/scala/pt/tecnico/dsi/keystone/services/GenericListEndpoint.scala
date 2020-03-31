package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.{Chunk, Stream}
import io.circe.{Decoder, HCursor}
import org.http4s.circe.decodeUri
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import org.http4s.{Header, Query, Uri}

abstract class GenericListEndpoint[F[_]](authToken: Header)(implicit client: Client[F], F: Sync[F]) {
  protected val dsl = new Http4sClientDsl[F] with Methods
  import dsl._

  def apply[R: Decoder](baseKey: String, uri: Uri, query: Query = Query.empty): Stream[F, R] = {
    implicit val paginatedDecoder: Decoder[(Option[Uri], List[R])] = (c: HCursor) => for {
      links <- c.downField("links").get[Option[Uri]]("next")
      objectList <- c.downField(baseKey).as[List[R]]
    } yield (links, objectList)

    Stream.unfoldChunkEval[F, Option[Uri], R](Some(uri)) {
      case Some(uri) =>
        for {
          request <- GET(uri.copy(query = uri.query ++ query.pairs), authToken)
          (next, entries) <- client.expect[(Option[Uri], List[R])](request)
        } yield Some((Chunk.iterable(entries), next))
      case None => F.pure(None)
    }
  }
}

object ListEndpoint {
  def apply[F[_]: Sync](authToken: Header)(implicit client: Client[F]): GenericListEndpoint[F] = new GenericListEndpoint[F](authToken) {}
}
