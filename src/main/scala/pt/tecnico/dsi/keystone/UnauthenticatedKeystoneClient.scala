package pt.tecnico.dsi.keystone

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.Json
import io.circe.syntax._
import org.http4s.syntax.string._
import org.http4s.Status.Successful
import org.http4s.{Header, Uri}
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import pt.tecnico.dsi.keystone.models.{Scope, Session}
import pt.tecnico.dsi.keystone.models.auth.Credential
import pt.tecnico.dsi.keystone.services._

class UnauthenticatedKeystoneClient[F[_]](val baseUri: Uri)(implicit client: Client[F], F: Sync[F]) {
  def authenticateWithPassword(credential: Credential): F[KeystoneClient[F]] = create(Left(credential))
  def authenticateWithPassword(credential: Credential, scope: Scope): F[KeystoneClient[F]] = create(Left(credential), Some(scope))

  def authenticateWithToken(token: String): F[KeystoneClient[F]] = create(Right(token))
  def authenticateWithToken(token: String, scope: Scope): F[KeystoneClient[F]] = create(Right(token), Some(scope))

  private def authBody(method: Either[Credential, String], scope: Option[Scope]): Json =
    Json.obj(
      "auth" -> Json.fromFields(Seq(
        "identity" -> Json.obj(
          "methods" -> Json.arr(method.fold(_ => "password", _ => "token").asJson),
          method.fold(
            credential => "password" -> Json.obj("user" -> credential.asJson),
            token => "token" -> Json.obj("id" -> token.asJson)
          )
        )
      ) ++ scope.map("scope" -> _.asJson))
    )

  private def create(method: Either[Credential, String], scope: Option[Scope] = None): F[KeystoneClient[F]] = {
    val dsl = new Http4sClientDsl[F] with Methods
    import dsl._

    client.fetch[(Header, Session)](POST(authBody(method, scope), baseUri / "v3" / "auth" / "tokens")) {
      case Successful(response) =>
        response.as[Session].flatMap { body =>
          response.headers.get("X-Subject-Token".ci) match {
            case Some(token) => F.pure((Header("X-Auth-Token", token.value), body))
            case None => F.raiseError(new IllegalStateException("Could not get X-Subject-Token from authentication response."))
          }
        }
      case failedResponse => F.raiseError(UnexpectedStatus(failedResponse.status))
    }.map { case (subjectToken, session) => new KeystoneClient(baseUri, session, subjectToken) }
  }
}
