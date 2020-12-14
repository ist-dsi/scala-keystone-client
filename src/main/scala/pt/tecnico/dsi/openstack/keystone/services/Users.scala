package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import io.circe.syntax._
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.{Group, KeystoneError, Project, Session, User}

/**
 * The service class for users.
 * @define domainModel user
 */
final class Users[F[_]: Sync: Client](baseUri: Uri, session: Session)
  extends CrudService[F, User, User.Create, User.Update](baseUri, "user", session.authToken)
  with UniqueWithinDomain[F, User]
  with EnableDisableEndpoints[F, User] {

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
           idpId: Option[String] = None, protocolId: Option[String] = None, uniqueId: Option[String] = None): F[List[User]] =
    list(Query(
      "name" -> name,
      "domain_ id" -> domainId,
      "password_expires_at" -> passwordExpiresAt,
      "enabled" -> enabled.map(_.toString),
      "idp_id" -> idpId,
      "protocol_id" -> protocolId,
      "unique_id" -> uniqueId,
    ))

  override def defaultResolveConflict(existing: User, create: User.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header]): F[User] = {
    // TODO: should we always update because of the password? We could try to authenticate with the user to check if the password is valid
    if (existing.defaultProjectId != create.defaultProjectId || existing.enabled != create.enabled) {
      val updated = User.Update(
        password = create.password,
        defaultProjectId = if (create.defaultProjectId != existing.defaultProjectId) create.defaultProjectId else None,
        enabled = Option(create.enabled).filter(_ != existing.enabled),
      )
      update(existing.id, updated, extraHeaders:_*)
    } else {
      Sync[F].pure(existing)
    }
  }
  override def createOrUpdate(create: User.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header] = Seq.empty)
    (resolveConflict: (User, User.Create) => F[User] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[User] = {
    val conflicting = """.*?Duplicate entry found with name ([^ ]+) at domain ID ([^.]+)\.""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders) {
      case KeystoneError(conflicting(name, domainId), Conflict.code, _) =>
        apply(name, domainId).flatMap { existing =>
          getLogger.info(s"createOrUpdate: found unique ${this.name} (id: ${existing.id}) with the correct name, on domain with id $domainId.")
          resolveConflict(existing, create)
        }
    }
  }
  
  override def update(id: String, update: User.Update, extraHeaders: Header*): F[User] =
    super.patch(wrappedAt, update, uri / id, extraHeaders:_*)
  
  override protected def updateEnable(id: String, enabled: Boolean): F[User] =
    update(id, User.Update(enabled = Some(enabled)))
  
  /**
   * Lists groups for a specified user
   *
   * @param id the user id
   * @return list of groups for a user
   */
  def listGroups(id: String): F[List[Group]] = super.list[Group]("groups", uri / id / "groups")
  
  /**
   * Lists groups for a specified user
   *
   * @param id the user id
   * @return list of groups for a user
   */
  def listProjects(id: String): F[List[Project]] = super.list[Project]("projects", uri / id / "projects")
  
  /**
   * @param id           the user identifier
   * @param originalPassword the original password
   * @param password         the new password
   */
  def changePassword(id: String, originalPassword: String, password: String): F[Unit] =
    super.post(wrappedAt, Map("password" -> password, "original_password" -> originalPassword).asJson, uri / id / "password")
}