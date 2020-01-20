package pt.tecnico.dsi.keystone

import cats.effect._
import org.http4s.implicits._
import cats.effect.Blocker
import java.util.concurrent._

import org.http4s.client.{Client, JavaNetClientBuilder}
import pt.tecnico.dsi.keystone.auth.models.request
import pt.tecnico.dsi.keystone.auth.models.request.{Auth, AuthTokenRequest, Domain, Identity }

object Main extends IOApp {
	val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
	val blocker: Blocker = Blocker.liftExecutorService(blockingPool)
	implicit val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

	def run(args: List[String]): IO[ExitCode] = {
		val keystoneClient = new KeystoneClient[IO](uri"http://localhost:5000")

		val authTokenRequest = AuthTokenRequest(
			Auth(
				Identity(
					List("password"),
					request.Password(
						request.User(
							name = "admin",
							Domain(
								name = "Default"
							),
							password = "ADMIN_PASS"
						)
					)
				)
			)
		)

		val result = keystoneClient.auth.tokens.authenticate(authTokenRequest).unsafeRunSync()
		println(result)

		IO(ExitCode.Success)
	}
}