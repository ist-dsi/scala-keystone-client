package pt.tecnico.dsi.openstack.keystone

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.derivation.{deriveEncoder, renaming}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, Printer}
import org.http4s.Method.POST
import org.http4s.Status.Successful
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s._
import org.typelevel.ci.CIString
import pt.tecnico.dsi.openstack.keystone.UnauthenticatedKeystoneClient.Credential
import pt.tecnico.dsi.openstack.keystone.models.{Scope, Session}

object UnauthenticatedKeystoneClient {
  object Credential {
    implicit val encoder: Encoder.AsObject[Credential] = deriveEncoder[Credential](renaming.snakeCase)

    def apply(id: String, password: String): Credential = new Credential(Some(id), None, password, None)
    def apply(name: String, password: String, domain: Scope.Domain): Credential = new Credential(None, Some(name), password, Some(domain))

    def fromEnvironment(env: Map[String, String]): Option[Credential] =
      env.get("OS_PASSWORD").flatMap { password =>
        val idOpt = env.get("OS_USER_ID").map(id => Credential(id, password))
        val nameOpt = env.get("OS_USERNAME").zip(Scope.Domain.fromEnvironment(env, "OS_USER"))
          .map { case (name, domain) => Credential(name, password, domain) }

        idOpt orElse nameOpt
      }
  }
  case class Credential(id: Option[String], name: Option[String], password: String, domain: Option[Scope.Domain])
}
class UnauthenticatedKeystoneClient[F[_]](baseUri: Uri)(implicit client: Client[F], F: Sync[F]) {
  /** Authenticates an identity and generates a token. Uses the password authentication method. Authorization is unscoped. */
  def authenticateWithPassword(userId: String, password: String): F[KeystoneClient[F]] =
    authenticate(Left(Credential(userId, password)))
  /** Authenticates an identity and generates a token. Uses the password authentication method and scopes authorization to `scope`. */
  def authenticateWithPassword(userId: String, password: String, scope: Scope): F[KeystoneClient[F]] =
    authenticate(Left(Credential(userId, password)), Some(scope))

  /** Authenticates an identity and generates a token. Uses the password authentication method. Authorization is unscoped. */
  def authenticateWithPassword(username: String, password: String, domain: Scope.Domain): F[KeystoneClient[F]] =
    authenticate(Left(Credential(username, password, domain)))
  /** Authenticates an identity and generates a token. Uses the password authentication method and scopes authorization to `scope`. */
  def authenticateWithPassword(username: String, password: String, domain: Scope.Domain, scope: Scope): F[KeystoneClient[F]] =
    authenticate(Left(Credential(username, password, domain)), Some(scope))

  /**
    * Authenticates an identity and generates a token. Uses the token authentication method. Authorization is unscoped.
    * @param token the token to use for authentication.
    */
  def authenticateWithToken(token: String): F[KeystoneClient[F]] = authenticate(Right(token))
  /**
    * Authenticates an identity and generates a token. Uses the token authentication method and scopes authorization to `scope`.
    * @param token the token to use for authentication.
    * @param scope the scope to which the authorization will be scoped to.
    */
  def authenticateWithToken(token: String, scope: Scope): F[KeystoneClient[F]] = authenticate(Right(token), Some(scope))

  //TODO: Authenticating with an Application Credential

  def authenticateFromEnvironment(env: Map[String, String]): F[KeystoneClient[F]] = {
    lazy val tokenOpt = env.get("OS_TOKEN").filter(_.nonEmpty)
    lazy val computedAuthType = tokenOpt.map(_ => "token").getOrElse("password")
    val scopeOpt = Scope.fromEnvironment(env)

    env.getOrElse("OS_AUTH_TYPE", computedAuthType).toLowerCase match {
      case "token" | "v3token" => tokenOpt match {
        case Some(token) => authenticate(Right(token), scopeOpt)
        case None => F.raiseError(new Throwable("To authenticate using the token method a token must be set in OS_TOKEN!"))
      }
      case "password" | "v3password" => Credential.fromEnvironment(env) match {
        case Some(credential) => authenticate(Left(credential), scopeOpt)
        case None => F.raiseError(new Throwable("To authenticate using the password method a valid credential must be set in OS_USER_ID/OS_USERNAME and OS_PASSWORD!"))
      }
      case m => F.raiseError(new Throwable(s"This library does not support authentication using $m"))
    }
  }

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
      ) ++ scope.map(s => "scope" -> s.asJson))
    )

  private def authenticate(method: Either[Credential, String], scope: Option[Scope] = None): F[KeystoneClient[F]] = {
    val dsl = new Http4sClientDsl[F] {}
    import dsl._
    val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    implicit def jsonEncoder[A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf(jsonPrinter)
    implicit def jsonDecoder[A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf
  
    val uri: Uri = if (baseUri.path.dropEndsWithSlash.toString.endsWith("v3")) baseUri else baseUri / "v3"
    POST(authBody(method, scope), uri / "auth" / "tokens").flatMap(client.run(_).use[F, Session] {
      case Successful(response) =>
        response.headers.get(CIString("X-Subject-Token")) match {
          case None => F.raiseError(new IllegalStateException("Could not get X-Subject-Token from authentication response."))
          case Some(token) =>
            implicit val sessionDecoder: EntityDecoder[F, Session] = jsonDecoder(Session.decoder(Header("X-Auth-Token", token.value)))
            response.as[Session]
        }
      case failedResponse => F.raiseError(new Throwable(s"unexpected HTTP status: ${failedResponse.status}"))
    }).map(new KeystoneClient(baseUri, _))
  }
}
