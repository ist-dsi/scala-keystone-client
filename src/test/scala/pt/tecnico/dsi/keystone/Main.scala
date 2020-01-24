package pt.tecnico.dsi.keystone

import cats.effect._
import org.http4s.implicits._
import cats.effect.Blocker
import java.util.concurrent._

import org.http4s.client.{Client, JavaNetClientBuilder}

object Main extends IOApp {
	val blockingPool: ExecutorService = Executors.newFixedThreadPool(5)
	val blocker: Blocker = Blocker.liftExecutorService(blockingPool)
	implicit val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

	def run(args: List[String]): IO[ExitCode] = {

	/*	val authTokenRequest = AuthTokenRequest(
			Auth(
				Identity(
					List("password"),
					Some(request.Password(
						request.User(
              id = None,
							name = "admin",
							Domain(
								name = Some("Default"),
                id = None
							),
							password = "ADMIN_PASS"
						)
					))
				),
				scope = Some(request.Scope(
						project = Some(request.Project(
							domain = Some(Domain(id=Some("default"), name=None)),
							id = None,
							name = Some("admin")
						)),
					system = None,
					domain = None
					)
				)
			)
		)
*/

		val clientBuilder = new ClientBuilder()
  		.endpoint(uri"http://localhost:5000")
  		.credentials("admin","ADMIN_PASS")
  		.scopeToProject("default", "admin")

		for {
			client <- clientBuilder.authenticate[IO]()
			_ <- IO { println(client.token) }
			list <- client.domains.list
			_ <- IO { println(list) }
		} yield ExitCode.Success
	}
}