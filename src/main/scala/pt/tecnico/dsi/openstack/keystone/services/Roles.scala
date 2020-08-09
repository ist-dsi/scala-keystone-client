package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.Status.Conflict
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import pt.tecnico.dsi.openstack.common.services.CrudService
import pt.tecnico.dsi.openstack.keystone.models.Role

final class Roles[F[_]: Sync: Client](baseUri: Uri, authToken: Header)
  extends CrudService[F, Role, Role.Create, Role.Update](baseUri, "role", authToken)
  with UniqueWithinDomain[F, Role] {

  /**
    * @param groupId filters the response by groupId
    * @return a stream of ids from projects to which a certain group has been assigned a certain role
    */
  def assignments(groupId: String): Stream[F, Role.Assignment] =
    list[Role.Assignment]("role_assignments", baseUri / "role_assignments", Query.fromPairs("group.id" -> groupId))

  /**
    * @param name filters the response by a role name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of roles filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): Stream[F, Role] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
    )))


  override def create(create: Role.Create, extraHeaders: Header*): F[Role] = createHandleConflict(create, extraHeaders:_*) {
    def updateIt(existingRole: Role): F[Role] = {
      // Description is the only field that can be different
      if (existingRole.description != create.description) {
        update(existingRole.id, Role.Update(description = create.description), extraHeaders:_*)
      } else {
        Sync[F].pure(existingRole)
      }
    }

    create.domainId match {
      case Some(domainId) =>
        // We got a Conflict and we have a domainId so we can find the existing Role since it must already exist
        get(create.name, domainId).flatMap(updateIt)
      case None =>
        // Currently Keystone does not accept the limit query param but it might in the future.
        // We only need 2 results to disambiguate whether the role name is unique or not.
        list(Query.fromPairs("name" -> create.name, "limit" -> "2"), extraHeaders:_*).compile.toList.flatMap { roles =>
          if (roles.lengthIs == 1) {
            updateIt(roles.head)
          } else {
            // There is more than one role with name `create.name`. We do not have enough information to disambiguate between them.
            Sync[F].raiseError(UnexpectedStatus(Conflict))
          }
        }
    }
  }
}
