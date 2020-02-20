package pt.tecnico.dsi.keystone

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.Session
import pt.tecnico.dsi.keystone.services._
import pt.tecnico.dsi.keystone.services.Credentials

object KeystoneClient {
  def apply[F[_]: Client: Sync](baseUri: Uri): UnauthenticatedKeystoneClient[F] = new UnauthenticatedKeystoneClient(baseUri)
}

class KeystoneClient[F[_]: Sync](val baseUri: Uri, val session: Session, val subjectToken: Header)
                                (implicit client: Client[F]) {
  val uri: Uri = baseUri / "v3"
  
  val authentication = new Authentication[F](uri, subjectToken)
  val credentials = new Credentials[F](uri, subjectToken)
  val domains = new Domains[F](uri, subjectToken)
  val users = new Users[F](uri, subjectToken)
  val groups = new Groups[F](uri, subjectToken)
  val regions = new Regions[F](uri, subjectToken)
  val projects = new Projects[F](uri, subjectToken)
}
