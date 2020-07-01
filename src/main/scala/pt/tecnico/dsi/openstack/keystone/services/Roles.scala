package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import cats.syntax.flatMap._
import fs2.Stream
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.{Header, Query, Uri}
import org.http4s.Status.Conflict
import pt.tecnico.dsi.openstack.common.models.WithId
import pt.tecnico.dsi.openstack.keystone.models.Role

final class Roles[F[_]: Sync: Client](baseUri: Uri, authToken: Header) extends CrudService[F, Role](baseUri, "role", authToken)
  with UniqueWithinDomain[F, Role] {

  /**
    * @param name filters the response by a role name.
    * @param domainId filters the response by a domain ID.
    * @return a stream of roles filtered by the various parameters.
    */
  def list(name: Option[String] = None, domainId: Option[String] = None): Stream[F, WithId[Role]] =
    list(Query.fromVector(Vector(
      "name" -> name,
      "domain_ id" -> domainId,
    )))


  override def create(role: Role): F[WithId[Role]] = createHandleConflict(role) {
    role.domainId match {
      case Some(domainId) =>
        get(role.name, domainId).flatMap(existingRole => update(existingRole.id, role))
      case None =>
        // Ideally we could limit the list to at most 2 results, because that is all we need to disambiguate whether the role is unique or not.
        listByName(role.name).compile.toList.flatMap { roles =>
          if (roles.lengthIs == 1) {
            update(roles.head.id, role)
          } else {
            implicitly[Sync[F]].raiseError(UnexpectedStatus(Conflict))
          }
        }
    }
  }
}
