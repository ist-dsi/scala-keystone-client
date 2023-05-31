package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Concurrent
import cats.syntax.flatMap.*
import org.http4s.Status.Conflict
import org.http4s.client.Client
import org.http4s.{Header, Query, Uri}
import org.log4s.getLogger
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.*

/**
 * The service class for roles.
 * @define domainModel role
 */
final class Roles[F[_]: Concurrent: Client](baseUri: Uri, session: Session)
  extends CrudService[F, Role, Role.Create, Role.Update](baseUri, "role", session.authToken)
    with UniqueWithinDomain[F, Role] {
  
  /**
    * @param name filters the response by a role name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of roles filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): F[List[Role]] =
    list(Query("name" -> name, "domain_id" -> domainId))
  
  /**
   * Get detailed information about the role specified by name and without a domain.
   *
   * @param name the role name
   * @return a Some(role) matching the name and without a domain if it exists. A None otherwise.
   */
  def getWithoutDomain(name: String): F[Option[Role]] =
    // The roles API allows two roles with the same name one with a domain and the other without a domain. However it does not
    // allow in list to filter by an empty domain. Hence this slightly ugly code.
    stream("name" -> name).filter(_.domainId.isEmpty).compile.last
  
  /**
   * Get detailed information about the role specified by name and without a domain, assuming it exists.
   *
   * @param name the role name
   * @return the role matching the name and without a domain. If no such role exists F will contain an error.
   */
  def applyWithoutDomain(name: String): F[Role] =
    getWithoutDomain(name).flatMap:
      case Some(role) => F.pure(role)
      case None => F.raiseError(new NoSuchElementException(s"""Could not find ${this.name} named "$name" without a domain."""))
  
  override def defaultResolveConflict(existing: Role, create: Role.Create, keepExistingElements: Boolean, extraHeaders: Seq[Header.ToRaw]): F[Role] =
    val updated = Role.Update(
      description = if create.description != existing.description then create.description else None,
    )
    if updated.needsUpdate then update(existing.id, updated, extraHeaders*)
    else Concurrent[F].pure(existing)
  override def createOrUpdate(create: Role.Create, keepExistingElements: Boolean = true, extraHeaders: Seq[Header.ToRaw] = Seq.empty)
    (resolveConflict: (Role, Role.Create) => F[Role] = defaultResolveConflict(_, _, keepExistingElements, extraHeaders)): F[Role] =
    val conflicting = """.*?Duplicate entry found with name ([^ ]+)(?: at domain ID ([^.]+))?\.""".r
    createHandleConflictWithError[KeystoneError](create, uri, extraHeaders):
      case error @ KeystoneError(conflicting(name, domainIdOpt), Conflict.code, _) =>
        Option(domainIdOpt) match
          case Some(domainId) =>
            // We got a Conflict and we have a domainId so we can find the existing Role since it must already exist
            apply(name, domainId).flatMap { existing =>
              getLogger.info(s"createOrUpdate: found unique ${this.name} (id: ${existing.id}) with the correct name, on domain with id $domainId.")
              resolveConflict(existing, create)
            }
          case None =>
            list(Query.fromPairs("name" -> create.name), extraHeaders*).flatMap { list =>
              // We know the domainId must be empty so we can use that to further refine the search
              list.filter(_.domainId.isEmpty) match
                case List(existing) =>
                  getLogger.info(s"createOrUpdate: found unique ${this.name} (id: ${existing.id}) with the correct name.")
                  resolveConflict(existing, create)
                case _ =>
                  // There is more than one role with name `create.name`. We do not have enough information to disambiguate between them.
                  Concurrent[F].raiseError(error)
            }
  
  override def update(id: String, update: Role.Update, extraHeaders: Header.ToRaw*): F[Role] =
    super.patch(wrappedAt, update, uri / id, extraHeaders*)
  
  /** Allows performing role assignment operations on the domain with `id` */
  def onDomain(id: String): RoleAssignment[F] = new RoleAssignment(baseUri, Scope.Domain.id(id), session)
  /** Allows performing role assignment operations on the project with `id` */
  def onProject(id: String): RoleAssignment[F] = new RoleAssignment(baseUri, Scope.Project(id), session)
  
  /** Allows performing role assignment operations on `domain`. */
  def on(domain: Domain): RoleAssignment[F] = onDomain(domain.id)
  /** Allows performing role assignment operations on `project`. */
  def on(project: Project): RoleAssignment[F] = onProject(project.id)
  /** Allows performing role assignment operations on system. */
  def on(system: System.type): RoleAssignment[F] = new RoleAssignment(baseUri, Scope.System(), session)
  
  def listAssignments(userId: Option[String] = None, groupId: Option[String] = None, roleId: Option[String] = None,
    domainId: Option[String] = None, projectId: Option[String] = None, system: Option[Boolean] = None,
    effective: Boolean = false, includeNames: Boolean = false, includeSubtree: Boolean = false): F[List[Assignment]] =
    val query: Vector[(String, Option[String])] = Vector(
      "user.id" -> userId,
      "group.id" -> groupId,
      "role.id" -> roleId,
      "scope.domain.id" -> domainId,
      "scope.project.id" -> projectId,
      "scope.system" -> system.map(_.toString),
    ).filter { case (_, value) => value.isDefined } ++
      Option.when(effective)("effective" -> Option.empty) ++
      Option.when(includeNames)("include_names" -> Option.empty) ++
      Option.when(includeSubtree)("include_subtree" -> Option.empty)
    
    list[Assignment]("role_assignments", (baseUri / "role_assignments").copy(query = Query.fromVector(query)))
  
  def listAssignmentsForUser(id: String): F[List[UserAssignment]] = listAssignments(userId = Some(id)).asInstanceOf[F[List[UserAssignment]]]
  def listAssignmentsForGroup(id: String): F[List[GroupAssignment]] = listAssignments(groupId = Some(id)).asInstanceOf[F[List[GroupAssignment]]]
  def listAssignmentsForRole(id: String): F[List[Assignment]] = listAssignments(roleId = Some(id))
  def listAssignments(user: User): F[List[UserAssignment]] = listAssignmentsForUser(user.id)
  def listAssignments(group: Group): F[List[GroupAssignment]] = listAssignmentsForGroup(group.id)
  def listAssignments(role: Role): F[List[Assignment]] = listAssignmentsForRole(role.id)
}