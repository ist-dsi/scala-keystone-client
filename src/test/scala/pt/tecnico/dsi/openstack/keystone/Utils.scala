package pt.tecnico.dsi.openstack.keystone

import cats.effect.unsafe.implicits.global // I'm sure there is a better way to do this. Please do not copy paste
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.util.Random
import cats.effect.{IO, Resource}
import cats.implicits.*
import org.http4s.client.Client
import org.http4s.blaze.client.BlazeClientBuilder
import org.scalatest.*
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

abstract class Utils extends AsyncWordSpec with Matchers with BeforeAndAfterAll:
  val (_httpClient, finalizer) = BlazeClientBuilder[IO]
    .withResponseHeaderTimeout(5.seconds)
    .withCheckEndpointAuthentication(false)
    .resource.allocated.unsafeRunSync()
  
  override protected def afterAll(): Unit = finalizer.unsafeRunSync()
  
  // The requests are being logged twice. And the response bodies are being logged twice.
  // This behavior is specific to the keystone client, the other clients work correctly.
  /* val b = for {
    _ <- c.statusFromString("http://127.0.0.1:35357/v3/") // Request logged once. Response body logged once.
    _ <- c.statusFromString("http://www.google.com/") // Request logged once. Response body logged once.
    _ <- c.statusFromString("http://127.0.0.1:35357/v3/") // Request logged **twice**. Response body once. ?!?!?
    _ <- c.statusFromString("http://www.google.com/") // Request logged once. Response body logged once.
  } yield ()
  b.unsafeRunSync() */
  
  /*import org.http4s.Headers
  import org.http4s.client.middleware.{Logger, ResponseLogger}
  import org.typelevel.ci.CIString
  given Client[IO] = Logger.colored(
    logHeaders = true,
    logBody = true,
    redactHeadersWhen = (Headers.SensitiveHeaders ++ List(CIString("X-Auth-Token"), CIString("X-Subject-Token"))).contains,
    responseColor = ResponseLogger.defaultResponseColor[IO] _
  )(_httpClient)*/
  given Client[IO] = _httpClient
  
  // This way we only authenticate to Openstack once, and make the logs smaller and easier to debug.
  val keystone: KeystoneClient[IO] = KeystoneClient.authenticateFromEnvironment {
    import scala.sys.process.*
    val ignoreStdErr = ProcessLogger(_ => ())
    val openstackEnvVariableRegex = """(?<=\+\+ )(OS_[A-Z_]+)=([^\n]+)""".r.unanchored
    "docker logs dev-keystone".lazyLines(ignoreStdErr).collect {
      case openstackEnvVariableRegex(key, value) => key -> value
    }.toMap + ("OS_AUTH_URL" -> "http://127.0.0.1:5000") // The docker logs do not expose this variable correctly
  }.unsafeRunSync()
  
  // Not very purely functional :(
  val random = new Random()
  
  def randomName(): String = random.alphanumeric.take(10).mkString.dropWhile(_.isDigit).toLowerCase
  
  def withRandomName[T](f: String => IO[T]): IO[T] = IO.delay(randomName()).flatMap(f)
  
  def resourceCreator[R <: Identifiable, Create](service: CrudService[IO, R, Create, ?])(create: String => Create): Resource[IO, R] =
    Resource.make(withRandomName(name => service(create(name))))(model => service.delete(model.id))
  
  extension [T](io: IO[T])
    def idempotently(test: T => Assertion, repetitions: Int = 3): IO[Assertion] =
      require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")
      io.flatMap { firstResult =>
        // If this fails we do not want to mask its exception with "Operation is not idempotent".
        // Because failing in the first attempt means whatever is being tested in `test` is not implemented correctly.
        test(firstResult)
        (2 to repetitions).toList.traverse { _ =>
          io
        } map { results =>
          // And now we want to catch the exception because if `test` fails here it means it is not idempotent.
          try
            results.foreach(test)
            succeed
          catch
            case e: TestFailedException =>
              val numberOfDigits = Math.floor(Math.log10(repetitions.toDouble)).toInt + 1
              val resultsString = (firstResult +: results).zipWithIndex
                .map { case (result, i) =>
                  s" %${numberOfDigits}d: %s".format(i + 1, result)
                }.mkString("\n")
              throw e.modifyMessage(_.map(message =>
                s"""Operation is not idempotent. Results:
                   |$resultsString
                   |$message""".stripMargin))
        }
      }
  
  given Conversion[IO[Assertion], Future[Assertion]] = _.unsafeToFuture()
