package pt.tecnico.dsi.openstack.keystone.models

import io.circe.Decoder.Result
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Codec, DecodingFailure, HCursor, Json}

object Credential {
  implicit val codec: Codec[Credential] = new Codec[Credential] {
    override def apply(a: Credential): Json = Json.obj(
      "type" -> "ec2".asJson,
      "blob" -> Map("access" -> a.access, "secret" -> a.secret).asJson.noSpaces.asJson,
      "project_id" -> a.projectId.asJson,
      "user_id" -> a.userId.asJson
    )

    override def apply(c: HCursor): Result[Credential] =
      for {
        blobJsonString <- c.get[String]("blob")
        blob <- decode[Json](blobJsonString).left.map(e => DecodingFailure(e.getMessage, c.downField("blob").history))
        access <- blob.hcursor.get[String]("access")
        secret <- blob.hcursor.get[String]("secret")
        projectId <- c.get[String]("project_id")
        userId <- c.get[String]("user_id")
      } yield Credential(access, secret, projectId, userId)
  }
}
case class Credential(access: String, secret: String, projectId: String, userId: String)