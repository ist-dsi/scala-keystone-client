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

class KeystoneClient[F[_]: Sync](val baseUri: Uri, val session: Session, val authToken: Header)
                                (implicit client: Client[F]) {

	val uri: Uri = baseUri / "v3"

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
