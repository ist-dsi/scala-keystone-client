package pt.tecnico.dsi.openstack.keystone.services

import fs2.Stream
import org.http4s.Query
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

// This would be really helpful here https://github.com/scala/bug/issues/9785
trait UniqueWithinDomain[F[_], T <: Identifiable] { this: CrudService[F, T, _, _] =>
  /** Lists `T`s with the given name.
    * @note Since the `T` name must be unique within a domain, all the returned `T`s will have different domains.
    * @param name the name to search for.
    */
  def listByName(name: String): Stream[F, T] = list(Query.fromPairs("name" -> name))

  /** Lists `T`s in the given domain.
    * @param domainId the domain id of the domain.
    */
  def listByDomain(domainId: String): Stream[F, T] = list(Query.fromPairs("domain_id" -> domainId))

  /**
    * Get detailed information about the `T` specified by name and domainId.
    *
    * @param name the `T` name
    * @param domainId the domain id
    * @return the `T` matching the name in a specific domain.
    */
  def get(name: String, domainId: String): F[T] = {
    // The name is unique within the owning domain.
    list(Query.fromPairs("name" -> name, "domain_id" -> domainId)).compile.lastOrError
  }
}