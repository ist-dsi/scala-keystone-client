package pt.tecnico.dsi.openstack.keystone.services

import cats.syntax.flatMap._
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

// This would be really helpful here https://github.com/scala/bug/issues/9785
trait UniqueWithinDomain[F[_], T <: Identifiable] { this: CrudService[F, T, _, _] =>
  /** Lists `T`s with the given name.
    * @note Since the `T` name must be unique within a domain, all the returned `T`s will have different domains.
    * @param name the name to search for.
    */
  def listByName(name: String): F[List[T]] = list("name" -> name)
  
  /** Lists `T`s in the given domain.
    * @param domainId the domain id of the domain.
    */
  def listByDomain(domainId: String): F[List[T]] = list("domain_id" -> domainId)
  
  /**
   * Get detailed information about the `T` specified by name and domainId.
   *
   * @param name the `T` name
   * @param domainId the domain id
   * @return a Some `T` matching the name in a specific domain if it exists. A None otherwise.
   */
  def get(name: String, domainId: String): F[Option[T]] = stream("name" -> name, "domain_id" -> domainId).compile.last
  
  /**
   * Get detailed information about the `T` specified by name and domainId, assuming it exists.
   *
   * @param name the `T` name
   * @param domainId the domain id
   * @return the `T` matching the name in a specific domain. If no such `T` exists F will contain an error.
   */
  def apply(name: String, domainId: String): F[T] =
    get(name, domainId).flatMap {
      case Some(model) => F.pure(model)
      case None =>
        F.raiseError(new NoSuchElementException(s"""Could not find ${this.name} named "$name" with domain id "$domainId""""))
    }
}