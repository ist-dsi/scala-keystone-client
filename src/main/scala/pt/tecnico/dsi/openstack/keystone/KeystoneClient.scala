package pt.tecnico.dsi.openstack.keystone

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.circe.{Decoder, Encoder, Json, Printer}
import io.circe.derivation.{deriveEncoder, renaming}
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.Status.Successful
import org.http4s.{EntityDecoder, EntityEncoder, Header, Request, Response, Uri, circe}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.typelevel.ci.CIString
import pt.tecnico.dsi.openstack.common.models.UnexpectedStatus
import pt.tecnico.dsi.openstack.keystone.models.{Scope, Session}
import pt.tecnico.dsi.openstack.keystone.services._

object KeystoneClient {
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
  case class Credential private (id: Option[String], name: Option[String], password: String, domain: Option[Scope.Domain])
  
  /** Authenticates an identity and generates a token. Uses the password authentication method. Authorization is unscoped. */
  def authenticateWithPassword[F[_]: Client: Sync](baseUri: Uri, userId: String, password: String): F[KeystoneClient[F]] =
    authenticate(baseUri, Left(Credential(userId, password)))
  /** Authenticates an identity and generates a token. Uses the password authentication method and scopes authorization to `scope`. */
  def authenticateWithPassword[F[_]: Client: Sync](baseUri: Uri, userId: String, password: String, scope: Scope): F[KeystoneClient[F]] =
    authenticate(baseUri, Left(Credential(userId, password)), Some(scope))
  
  /** Authenticates an identity and generates a token. Uses the password authentication method. Authorization is unscoped. */
  def authenticateWithPassword[F[_]: Client: Sync](baseUri: Uri, username: String, password: String, domain: Scope.Domain): F[KeystoneClient[F]] =
    authenticate(baseUri, Left(Credential(username, password, domain)))
  /** Authenticates an identity and generates a token. Uses the password authentication method and scopes authorization to `scope`. */
  def authenticateWithPassword[F[_]: Client: Sync](baseUri: Uri, username: String, password: String, domain: Scope.Domain, scope: Scope): F[KeystoneClient[F]] =
    authenticate(baseUri, Left(Credential(username, password, domain)), Some(scope))
  
  /**
   * Authenticates an identity and generates a token. Uses the token authentication method. Authorization is unscoped.
   * @param token the token to use for authentication.
   */
  def authenticateWithToken[F[_]: Client: Sync](baseUri: Uri, token: String): F[KeystoneClient[F]] =
    authenticate(baseUri, Right(token))
  /**
   * Authenticates an identity and generates a token. Uses the token authentication method and scopes authorization to `scope`.
   * @param token the token to use for authentication.
   * @param scope the scope to which the authorization will be scoped to.
   */
  def authenticateWithToken[F[_]: Client: Sync](baseUri: Uri, token: String, scope: Scope): F[KeystoneClient[F]] =
    authenticate(baseUri, Right(token), Some(scope))
  
  /** Authenticates using the environment variables. */
  def authenticateFromEnvironment[F[_]: Client: Sync](env: Map[String, String] = sys.env): F[KeystoneClient[F]] = {
    lazy val tokenOpt = env.get("OS_TOKEN").filter(_.nonEmpty)
    lazy val computedAuthType = tokenOpt.map(_ => "token").getOrElse("password")
    val scopeOpt = Scope.fromEnvironment(env)
    
    def error[A](message: String): F[A] = Sync[F].raiseError(new Throwable(message))
    
    for {
      authUrl <- Sync[F].fromOption(env.get("OS_AUTH_URL"), new Throwable(s"Could not get OS_AUTH_URL from the environment"))
      baseUri <- Sync[F].fromEither(Uri.fromString(authUrl))
      client <- env.getOrElse("OS_AUTH_TYPE", computedAuthType).toLowerCase match {
        case "token" | "v3token" => tokenOpt match {
          case Some(token) => authenticate(baseUri, Right(token), scopeOpt)
          case None => error("To authenticate using the token method a token must be set in OS_TOKEN!")
        }
        case "password" | "v3password" => Credential.fromEnvironment(env) match {
          case Some(credential) => authenticate(baseUri, Left(credential), scopeOpt)
          case None => error("To authenticate using the password method a credential must be set in OS_USER_ID/OS_USERNAME and OS_PASSWORD!")
        }
        case m => error(s"This library does not support authentication using $m")
      }
    } yield client
  }
  
  /**
   * Creates the Json object used to authenticate in Openstack.
   * @param method the authentication method to use. Currently only credential and token are accepted.
   * @param scope the scope to use. Setting it to `Some(Scope.Unscoped)` will cause an explicit unscoped authorization.
   *              Setting it to `None` will cause an unscoped authorization.
   */
  def authenticationBody(method: Either[Credential, String], scope: Option[Scope]): Json =
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
  
  /**
   * Authenticate against a Keystone located at `baseUri` using `method` and the provided `scope`.
   * @param baseUri the base uri in which Keystone is accessible.
   * @param method the authentication method to use. Currently only credential and token are accepted.
   * @param scope the scope to use. Setting it to `Some(Scope.Unscoped)` will cause an explicit unscoped authorization.
   *              Setting it to `None` will cause an unscoped authorization.
   * @param client the http client to use.
   * @tparam F the effect type
   * @return On a successful authentication an `F` with a `KeystoneClient`. On failure `F` will contain an error.
   */
  def authenticate[F[_]](baseUri: Uri, method: Either[Credential, String], scope: Option[Scope] = None)
    (implicit client: Client[F], F: Sync[F]): F[KeystoneClient[F]] = {
    
    val dsl = new Http4sClientDsl[F] {}
    import dsl._
    
    val jsonPrinter: Printer = Printer.noSpaces.copy(dropNullValues = true)
    implicit def jsonEncoder[A: Encoder]: EntityEncoder[F, A] = circe.jsonEncoderWithPrinterOf(jsonPrinter)
    implicit def jsonDecoder[A: Decoder]: EntityDecoder[F, A] = circe.accumulatingJsonOf
    
    def defaultOnError[R](request: Request[F], response: Response[F]): F[R] = for {
      requestBody: String <- request.bodyText.compile.foldMonoid
      responseBody: String <- response.bodyText.compile.foldMonoid
      // The defaultOnError implemented in the DefaultClient is not very helpful to debug problems
      // Most notably it does not show the body of the response when the request is not successful.
      // https://github.com/http4s/http4s/issues/3707
      // So we created our own UnexpectedStatus with a much more detailed information
      result <- F.raiseError[R](UnexpectedStatus(request.method, request.uri, requestBody, response.status, responseBody))
    } yield result
    
    val uri: Uri = if (baseUri.path.dropEndsWithSlash.toString.endsWith("v3")) baseUri else baseUri / "v3"
    
    val request = POST(authenticationBody(method, scope), uri / "auth" / "tokens")
    client.run(request).use[F, Session] {
      case Successful(response) =>
        response.headers.get(CIString("X-Subject-Token")) match {
          case None => F.raiseError(new IllegalStateException("Could not get X-Subject-Token from authentication response."))
          case Some(token) =>
            implicit val sessionDecoder: Decoder[Session] = Session.decoder(Header("X-Auth-Token", token.value))
            response.as[Session]
        }
      case failedResponse => defaultOnError(request, failedResponse)
    }.map(session => new KeystoneClient(uri, session)(client, F))
  }
}
class KeystoneClient[F[_]: Client: Sync](val uri: Uri, val session: Session) {
  val authentication = new Authentication[F](uri, session)
  val domains = new Domains[F](uri, session)
  val groups = new Groups[F](uri, session)
  val projects = new Projects[F](uri, session)
  val regions = new Regions[F](uri, session)
  val roles = new Roles[F](uri, session)
  val services = new Services[F](uri, session)
  val endpoints = new Endpoints[F](uri, session)
  val users = new Users[F](uri, session)
}
