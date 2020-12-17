package pt.tecnico.dsi.openstack.keystone

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.openstack.keystone.models.Session
import pt.tecnico.dsi.openstack.keystone.services._

object KeystoneClient {
  def fromEnvironment[F[_]: Client: Sync](env: Map[String, String] = sys.env): F[KeystoneClient[F]] = for {
    authUrl <- Sync[F].fromOption(env.get("OS_AUTH_URL"), new Throwable(s"Could not get OS_AUTH_URL from the environment"))
    baseUri <- Sync[F].fromEither(Uri.fromString(authUrl))
    client <- apply(baseUri).authenticateFromEnvironment(env)
  } yield client
  
  def apply[F[_]: Client: Sync](baseUri: Uri): UnauthenticatedKeystoneClient[F] = {
    val uri: Uri = if (baseUri.path.dropEndsWithSlash.toString.endsWith("v3")) baseUri else baseUri / "v3"
    new UnauthenticatedKeystoneClient(uri)
  }
}
class KeystoneClient[F[_]: Sync: Client] private[keystone] (val uri: Uri, val session: Session) {
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
