package pt.tecnico.dsi.keystone

import cats.effect.Sync
import cats.syntax.flatMap._
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.keystone.models.Session
import pt.tecnico.dsi.keystone.services._

object KeystoneClient {
  def fromEnvironment[F[_]: Client](env: Map[String, String] = sys.env)(implicit F: Sync[F]): F[KeystoneClient[F]] = {
    F.fromOption(env.get("OS_AUTH_URL"), new Throwable(s"Could not get OS_AUTH_URL from the environment"))
      .flatMap(authUrl => F.fromEither(Uri.fromString(authUrl)))
      .flatMap(baseUri => apply(baseUri).authenticateFromEnvironment(env))
  }

  def apply[F[_]: Client: Sync](baseUri: Uri): UnauthenticatedKeystoneClient[F] = new UnauthenticatedKeystoneClient(baseUri)
}

class KeystoneClient[F[_]: Sync](val baseUri: Uri, val session: Session, val authToken: Header)(implicit client: Client[F]) {
  val uri: Uri = if (baseUri.path.endsWith("v3") || baseUri.path.endsWith("v3/")) baseUri else baseUri / "v3"

  val authentication = new Authentication[F](uri, authToken)
  val credentials = new Credentials[F](uri, authToken)
  val domains = new Domains[F](uri, authToken)
  val groups = new Groups[F](uri, authToken)
  val projects = new Projects[F](uri, authToken)
  val regions = new Regions[F](uri, authToken)
  val roles = new Roles[F](uri, authToken)
  val services = new Services[F](uri, authToken)
  val endpoints = new Endpoints[F](uri, authToken)
  val users = new Users[F](uri, authToken)
}
