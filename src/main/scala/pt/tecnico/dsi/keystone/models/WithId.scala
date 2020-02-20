package pt.tecnico.dsi.keystone.models

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._

object WithId {
  implicit def toModel[A](modelWithId: WithId[A]): A = modelWithId.model

  implicit def decoder[T: Decoder]: Decoder[WithId[T]] = (c: HCursor) => for {
    id <- c.get[String]("id")
    model <- c.as[T]
  } yield WithId(id, model)

  implicit def encoder[T: Encoder]: Encoder[WithId[T]] = (a: WithId[T]) => a.model.asJson.mapObject(_.add("id", a.id.asJson))
}
// All Openstack IDs are strings
case class WithId[A](id: String, model: A)