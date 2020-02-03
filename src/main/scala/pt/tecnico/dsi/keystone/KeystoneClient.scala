package pt.tecnico.dsi.keystone

import cats.effect.Sync
import cats.implicits._
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.auth.request.AuthTokenRequest
import pt.tecnico.dsi.keystone.models.auth.response.Token
import pt.tecnico.dsi.keystone.services.{DomainService, GroupService, ProjectService, RegionService, TokenService, UserService}

object KeystoneClient {

	/**
		* Create an authenticated Keystone client.
		*
		*/
	def create[F[_]: Sync](baseUri: Uri, auth: AuthTokenRequest)(implicit client: Client[F]): F[KeystoneClient[F]] = {
		val token = new TokenService[F](baseUri.withPath("/v3/auth/tokens"))
		for {
			(response, header) <- token.authenticate(auth)
		} yield new KeystoneClient(baseUri, response.token,  Header("X-Auth-Token", header))
	}

}

class KeystoneClient[F[_]: Sync](val baseUri: Uri, val authenticatedUser: Token, val token: Header)
																(implicit client: Client[F]) {

	val uri: Uri = baseUri / "v3"
	val domains = new DomainService[F](uri / "domains", token)
	val users 	= new UserService[F](uri / "users", this)
	val groups 	= new GroupService[F](uri / "groups", token)
	val regions = new RegionService[F](uri / "regions", token)
	val projects = new ProjectService[F](uri / "projects", token)

}



