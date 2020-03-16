package pt.tecnico.dsi.keystone.models

import io.circe.syntax._
import io.circe.parser.decode
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

object Credential {
  implicit val decoder: Decoder[Credential] = (c: HCursor) => for {
    blobJsonString <- c.get[String]("blob")
    blob <- decode[Json](blobJsonString).left.map(e => DecodingFailure(e.getMessage, c.downField("blob").history))
    access <- blob.hcursor.get[String]("access")
    secret <- blob.hcursor.get[String]("secret")
    projectId <- c.get[String]("project_id")
    userId <- c.get[String]("user_id")
  } yield Credential(access, secret, projectId, userId)

  implicit val encoder: Encoder[Credential] = (a: Credential) => Json.obj(
    "type" -> "ec2".asJson,
    "blob" -> Map("access" -> a.access, "secret" -> a.secret).asJson.noSpaces.asJson,
    "project_id" -> a.projectId.asJson,
    "user_id" -> a.userId.asJson
  )
}
case class Credential(access: String, secret: String, projectId: String, userId: String)