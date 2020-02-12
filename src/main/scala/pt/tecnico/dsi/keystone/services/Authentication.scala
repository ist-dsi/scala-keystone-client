package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s.client.Client
import org.http4s.{Header, Uri}

class Authentication[F[_]: Sync](baseUri: Uri, subjectToken: Header)(implicit client: Client[F]) {
	val uri: Uri = baseUri / "auth"

	/*
	def paginatedDecoder[T: Decoder](rootKey: String): Decoder[(Option[Uri], List[T])] = (c: HCursor) => for {
		links <- c.downField("links").get[Option[Uri]]("next")
		domainObjectList <- c.downField(rootKey).as[List[T]]
	} yield (links, domainObjectList)

	def list[T: Decoder](rootKey: String, query: Query = Query.empty): Stream[F, T] = {
		import org.http4s.circe.accumulatingJsonOf
		val decoder = accumulatingJsonOf[F, (Option[Uri], List[T])](implicitly[Sync[F]], paginatedDecoder[T](rootKey))
		Stream.unfoldChunkEval[F, Option[Uri], T](Some(uri)) {
			case Some(uri) =>
				for {
					request <- GET(uri.copy(query = uri.query ++ query.pairs), subjectToken)
					(next, entries) <- client.expect[(Option[Uri], List[T])](request)(decoder)
				} yield Some((Chunk.iterable(entries), next))
			case None => Sync[F].pure(None)
		}
	}
	def serviceCatalog: F[List[CatalogEntry]] = new CRUDService[F, CatalogEntry](baseUri, "catalog", subjectToken) {
		override protected val plural: String = "catalog"
	}.list()
	*/
}
