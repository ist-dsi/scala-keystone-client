package pt.tecnico.dsi.keystone

import org.http4s.implicits._
import cats.effect.Sync
import org.http4s.Uri
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.auth.request.{Auth, AuthTokenRequest, Domain, Identity, Password, Project, Scope, User}

class ClientBuilder {

  private var authUrl: Uri        = uri"http://localhost:5000"
  private var domainId: String    = "default"
  private var password: String    = "ADMIN_PASS"
  private var username: String    = "admin"
  private var projectName: String = "admin"
  private var scoped: Boolean     = false

  def endpoint(uri: Uri): ClientBuilder = {
    authUrl = uri
    this
  }

  def credentials(name: String, password: String): ClientBuilder = {
    this.username = username
    this.password = password
    this
  }

  def scopeToProject(domainId: String, projectName: String): ClientBuilder = {
    this.domainId = domainId
    this.projectName = projectName
    this.scoped = true
    this
  }

  def authenticate[F[_]: Sync]()
    (implicit client: Client[F]): F[KeystoneClient[F]] = {

    val domain = Domain(
      name = None,
      id = Some(domainId)
    )

    val user = User(
      id = None,
      name = username,
      password = password,
      domain = domain
    )

    val identity = Identity(
      List("password"),
      Some(
        Password(user)
      )
    )

    val project = Project(
      id = None,
      name = Some(projectName),
      domain = Some(domain)
    )

    val scope = Scope(
      system = None,
      domain = None,
      project = Some(project)
    )

    val auth = Auth(
      identity,
      if (scoped) Some(scope) else None
    )

    val authTokenRequest = AuthTokenRequest(
      auth
    )

    KeystoneClient.create[F](authUrl, authTokenRequest)
  }

}
