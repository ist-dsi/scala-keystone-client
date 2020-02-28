package pt.tecnico.dsi.keystone.models

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.http4s.Uri
import org.http4s.circe.decodeUri

object WithId {
  implicit def toModel[A](modelWithId: WithId[A]): A = modelWithId.model

  implicit def decoder[T: Decoder]: Decoder[WithId[T]] = (c: HCursor) => for {
    id <- c.get[String]("id")
    link <- c.downField("links").get[Option[Uri]]("self")
    model <- c.as[T]
  } yield WithId(id, model, link)

  implicit def encoder[T: Encoder]: Encoder[WithId[T]] = (a: WithId[T]) => a.model.asJson.mapObject(_.add("id", a.id.asJson))
}
// All Openstack IDs are strings
case class WithId[A](id: String, model: A, link: Option[Uri])