package pt.tecnico.dsi.keystone

import cats.Applicative
import cats.effect.Sync
import io.circe.{Codec, Decoder, Encoder, Printer}
import org.http4s.{EntityDecoder, EntityEncoder, circe}

package object services {
  implicit def jsonDecoder[F[_]: Sync, A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf[F, A]
  val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
  implicit def jsonEncoder[F[_]: Applicative, A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf[F, A](jsonPrinter)

  implicit def codecFromEncoderAndDecoder[A](implicit decoder: Decoder[A], encoder: Encoder[A]): Codec[A] =
    Codec.from(decoder, encoder)

  // Without this decoding to Unit wont work. This makes the EntityDecoder[F, Unit] defined in EntityDecoder companion object
  // have a higher priority than the jsonDecoder defined above. https://github.com/http4s/http4s/issues/2806
  implicit def void[F[_]: Sync]: EntityDecoder[F, Unit] = EntityDecoder.void
}