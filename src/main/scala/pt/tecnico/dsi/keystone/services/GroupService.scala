package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import cats.syntax.functor._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.impl.Methods
import pt.tecnico.dsi.keystone.models.groups.{Group, GroupWrapper, Groups}

class GroupService[F[_]: Sync](uri: Uri, token: Header)(implicit client: Client[F]) {

  private val dsl = new Http4sClientDsl[F] with Methods {}
  import dsl._

  /**
    * Gets detailed information about a specified group by id
    * @param groupId the group id
    * @return the group
    */
  def get(groupId: String): F[Group] = {
    client.expect[GroupWrapper](
      GET(uri / groupId, token)
    ).map(_.group)
  }

  /**
    * Gets detailed information about groups matching specified by name and domain
    *
    * @param groupName the group name
    * @return the of list groups matching the name across all domains
    */
  def getByName(groupName: String): F[Seq[Group]] = {
    client.expect[Groups](
      GET(uri.withQueryParam("name", groupName), token)
    ).map(_.groups)
  }

  /**
    * Get detailed information about groups matching specified by name and domain
    *
    * @param groupName the group name
    * @param domainId  the domain id
    * @return the list of groups matching the name in a specific domain or null if not found
    */
  def get(groupName: String, domainId: String): F[Group] = {
    client.expect[GroupWrapper](GET(uri
      .withQueryParam("domain_id", domainId)
      .withQueryParam("name", groupName),
      token
    )).map(_.group)
  }

  /**
    * Deletes a group with a certain id.
    *
    * @param groupId
    */
  def delete(groupId: String): F[Unit] = {
    client.expect(DELETE(uri / groupId, token))
  }

  /**
    * Updates an existing group
    *
    * @param group the group set to update
    * @return the updated group
    */
  def update(group: Group): F[Group] = {
    client.expect[GroupWrapper](
      PATCH(GroupWrapper(group), uri / group.id, token)
    ).map(_.group)
  }

  /**
    * Creates a new group
    *
    * @param group the group
    * @return the newly created group
    */
  def create(group: Group): F[Group] = {
    client.expect[GroupWrapper](
      POST(GroupWrapper(group), uri, token)
    ).map(_.group)
  }

  /**
    * Lists of groups.
    *
    * @returns list of groups.
    */
  def list(): F[Seq[Group]] = {
    client.expect[Groups](
      GET(uri, token)
    ).map(_.groups)
  }

}
