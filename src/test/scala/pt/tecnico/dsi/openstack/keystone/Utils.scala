package pt.tecnico.dsi.openstack.keystone

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random
import cats.effect.{ContextShift, IO, Resource, Timer}
import cats.implicits._
import org.http4s.Headers
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.scalatest._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.typelevel.ci.CIString
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

abstract class Utils extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  implicit override def executionContext: ExecutionContextExecutor = ExecutionContext.global
  
  implicit val timer: Timer[IO] = IO.timer(executionContext)
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)
  
  val (_httpClient, finalizer) = BlazeClientBuilder[IO](executionContext)
    .withResponseHeaderTimeout(5.seconds)
    .withCheckEndpointAuthentication(false)
    .resource.allocated.unsafeRunSync()
  
  override protected def afterAll(): Unit = finalizer.unsafeRunSync()
  
  def createLogger(client: Client[IO], logMessages: Boolean): Client[IO] = {
    if (logMessages) {
      val logHeaders = true
      val logBody = true
      val redactHeadersWhen: CIString => Boolean =
        (Headers.SensitiveHeaders ++ List(CIString("X-Auth-Token"), CIString("X-Subject-Token"))).contains
      def log(s: String, color: String): IO[Unit] = IO(org.log4s.getLogger.info(s"$color$s${Console.RESET}"))
      
      // https://github.com/http4s/http4s/issues/4019
      val c = ResponseLogger.apply(logHeaders, logBody, redactHeadersWhen, Some(log(_, Console.BLUE)))(
        RequestLogger.apply(logHeaders, logBody, redactHeadersWhen, Some(log(_, Console.GREEN)))(
          client
        )
      )
      
      // The requests are being logged twice. And the response bodies are being logged twice.
      /*
      val b = for {
        _ <- c.statusFromString("http://127.0.0.1:35357/v3/") // Request logged once. Response body logged once.
        _ <- c.statusFromString("http://www.google.com/") // Request logged once. Response body logged once.
        _ <- c.statusFromString("http://127.0.0.1:35357/v3/") // Request logged **twice**. Response body once. ?!?!?
        _ <- c.statusFromString("http://www.google.com/") // Request logged once. Response body logged once.
      } yield ()
      b.unsafeRunSync()
      */
      c
    } else client
  }
  implicit val httpClient: Client[IO] = createLogger(_httpClient, logMessages = true)
  
  // This way we only authenticate to Openstack once, and make the logs smaller and easier to debug.
  val keystone: KeystoneClient[IO] = KeystoneClient.fromEnvironment {
    import scala.sys.process._
    val ignoreStdErr = ProcessLogger(_ => ())
    val openstackEnvVariableRegex = """(?<=\+\+ )(OS_[A-Z_]+)=([^\n]+)""".r.unanchored
    "docker logs dev-keystone".lazyLines(ignoreStdErr).collect {
      case openstackEnvVariableRegex(key, value) => key -> value
    }.toMap
  }.unsafeRunSync()
  
  // Not very purely functional :(
  val random = new Random()
  def randomName(): String = random.alphanumeric.take(10).mkString.dropWhile(_.isDigit).toLowerCase
  def withRandomName[T](f: String => IO[T]): IO[T] = IO.delay(randomName()).flatMap(f)
  
  def resourceCreator[R <: Identifiable, Create](service: CrudService[IO, R, Create, _])(create: String => Create): Resource[IO, R] = {
    Resource.make(withRandomName(name => service(create(name))))(model => service.delete(model.id))
  }
  
  implicit class RichIO[T](io: IO[T]) {
    def idempotently(test: T => Assertion, repetitions: Int = 3): IO[Assertion] = {
      require(repetitions >= 2, "To test for idempotency at least 2 repetitions must be made")
      io.flatMap { firstResult =>
        // If this fails we do not want to mask its exception with "Operation is not idempotent".
        // Because failing in the first attempt means whatever is being tested in `test` is not implemented correctly.
        test(firstResult)
        (2 to repetitions).toList.traverse { _ =>
          io
        } map { results =>
          // And now we want to catch the exception because if `test` fails here it means it is not idempotent.
          try {
            results.foreach(test)
            succeed
          } catch {
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
      }
    }
  }
  
  import scala.language.implicitConversions
  implicit def io2Future(io: IO[Assertion]): Future[Assertion] = io.unsafeToFuture()
}
