package pt.tecnico.dsi.keystone.models

import io.circe.syntax._
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
import org.http4s.Uri
import org.http4s.circe.decodeUri

object Credential {
  implicit val decoder: Decoder[Credential] = (c: HCursor) => for {
    blobJsonString <- c.get[String]("blob")
    blob <- decode[Json](blobJsonString).left.map(e => DecodingFailure(e.getMessage, c.downField("blob").history))
    access <- blob.hcursor.get[String]("access")
    secret <- blob.hcursor.get[String]("secret")
    projectId <- c.get[String]("project_id")
    userId <- c.get[String]("user_id")
    self <- c.downField("links").get[Option[Uri]]("self")
  } yield Credential(access, secret, projectId, userId, self)

  implicit val encoder: Encoder[Credential] = (a: Credential) => Json.obj(
    "type" -> "ec2".asJson,
    "blob" -> Map("access" -> a.access, "secret" -> a.secret).asJson.noSpaces.asJson,
    "project_id" -> a.projectId.asJson,
    "user_id" -> a.userId.asJson
  )
}
case class Credential(access: String, secret: String, projectId: String, userId: String, self: Option[Uri] = None)