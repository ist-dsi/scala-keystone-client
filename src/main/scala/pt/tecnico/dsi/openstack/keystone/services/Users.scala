package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import io.circe.syntax._
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Group, Project, Session, User}

final class Users[F[_]: Sync: Client](baseUri: Uri, session: Session, authToken: Header)
  extends CrudService[F, User, User.Create, User.Update](baseUri, "user", authToken)
  with UniqueWithinDomain[F, User]
  with EnableDisableEndpoints[F, User] {

  import dsl._

  /**
    *
    * @param name filters the response by a user name.
    * @param domainId filters the response by a domain ID.
    * @param passwordExpiresAt filter results based on which user passwords have expired.
    * @param enabled filters the response by either enabled (true) or disabled (false) users.
    * @param idpId filters the response by an identity provider ID.
    * @param protocolId filters the response by a protocol ID.
    * @param uniqueId filters the response by a unique ID.
    * @return a stream of users filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None, passwordExpiresAt: Option[String], enabled: Option[Boolean],
           idpId: Option[String] = None, protocolId: Option[String] = None, uniqueId: Option[String] = None): Stream[F, User] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
      "password_expires_at" -> passwordExpiresAt,
      "enabled" -> enabled.map(_.toString),
      "idp_id" -> idpId,
      "protocol_id" -> protocolId,
      "unique_id" -> uniqueId,
    )))

  /**
    * Lists groups for a specified user
    *
    * @param id the user id
    * @return list of groups for a user
    */
  def listGroups(id: String): Stream[F, Group] = super.list[Group]("groups", uri / id / "groups", Query.empty)

  /**
    * Lists groups for a specified user
    *
    * @param id the user id
    * @return list of groups for a user
    */
  def listProjects(id: String): Stream[F, Project] = super.list[Project]("projects", uri / id / "projects", Query.empty)

  /**
    * @param id           the user identifier
    * @param originalPassword the original password
    * @param password         the new password
    */
  def changePassword(id: String, originalPassword: String, password: String): F[Unit] = {
    val body = Map("user" -> Map(
      "password" -> password,
      "original_password" -> originalPassword,
    ))
    client.expect(POST(body.asJson, uri / id / password, authToken))
  }

  override def create(create: User.Create, extraHeaders: Header*): F[User] = createHandleConflict(create, extraHeaders:_*) {
    val domainId = create.domainId.getOrElse(domainIdFromScope(session.scope))
    // We got a Conflict so we must be able to find the existing User
    get(create.name, domainId).flatMap { existingUser =>
      // TODO: should we always update because of the password?
      if (existingUser.defaultProjectId != create.defaultProjectId || existingUser.enabled != create.enabled) {
        val updated = User.Update(password = create.password, defaultProjectId = create.defaultProjectId, enabled = Some(create.enabled))
        update(existingUser.id, updated, extraHeaders:_*)
      } else {
        Sync[F].pure(existingUser)
      }
    }
  }

  override protected def updateEnable(id: String, enabled: Boolean): F[User] = update(id, User.Update(enabled = Some(enabled)))
}