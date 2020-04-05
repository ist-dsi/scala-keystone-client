package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.keystone.services.RoleAssignment

trait RoleAssignmentSpec[F] {
  this: CRUDSpec[F] =>

  def roleService: KeystoneClient[IO] => RoleAssignment[IO]

  def groupStub = IO.pure(Group(
    name = "test-group",
    description = "test-desc",
    domainId = "default"
  ))

  def userStub = IO.pure(User(
    name = "test-user",
    domainId = "default"
  ))

  def roleStub = IO.pure(Role(
    name = "test-role2",
    description = Some("some-description"),
    domainId = None, // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
  ))

  s"The ${name} service" should {

    s"list roles for a group in a ${name}" in {
      for {
        client <- scopedClient
        obj <- stub
        group <- groupStub
        objWithId <- service(client).create(obj)
        groupWithId <- client.groups.create(group)
        roles <- roleService(client).roles.groups.list(objWithId.id, groupWithId.id).compile.toList
      } yield true shouldBe true
    }

    s"check group role assignment" in {
      for {
        client <- scopedClient
        obj <- stub
        role <- roleStub
        group <- groupStub
        roleWithId <- client.roles.create(role)
        objWithId <- service(client).create(obj)
        groupWithId <- client.groups.create(group)
        isIdempotent <- roleService(client).roles.groups.check(objWithId.id, groupWithId.id, roleWithId.id).valueShouldIdempotentlyBe(false)
      } yield isIdempotent
    }

    s"assign role to a group" in {
      for {
        client <- scopedClient
        obj <- stub
        role <- roleStub
        group <- groupStub
        roleWithId <- client.roles.create(role)
        objWithId <- service(client).create(obj)
        groupWithId <- client.groups.create(group)
        isIdempotent <- roleService(client).roles.groups.assign(objWithId.id, groupWithId.id, roleWithId.id).valueShouldIdempotentlyBe(())
      } yield isIdempotent
    }

    s"delete group role assignment" in {
      for {
        client <- scopedClient
        obj <- stub
        role <- roleStub
        group <- groupStub
        roleWithId <- client.roles.create(role)
        objWithId <- service(client).create(obj)
        groupWithId <- client.groups.create(group)
        isIdempotent <- roleService(client).roles.groups.delete(objWithId.id, groupWithId.id, roleWithId.id).valueShouldIdempotentlyBe(())
      } yield isIdempotent
    }

    //#region users
      s"list roles for a user in a ${name}" in {
        for {
          client <- scopedClient
          obj <- stub
          user <- userStub
          objWithId <- service(client).create(obj)
          userWithId <- client.users.create(user)
          roles <- roleService(client).roles.users.list(objWithId.id, userWithId.id).compile.toList
        } yield true shouldBe true
      }

      s"check user role assignment" in {
        for {
          client <- scopedClient
          obj <- stub
          role <- roleStub
          user <- userStub
          roleWithId <- client.roles.create(role)
          objWithId <- service(client).create(obj)
          userWithId <- client.users.create(user)
          isIdempotent <- roleService(client).roles.users.check(objWithId.id, userWithId.id, roleWithId.id).valueShouldIdempotentlyBe(false)
        } yield isIdempotent
      }

      s"assign role to a user" in {
        for {
          client <- scopedClient
          obj <- stub
          role <- roleStub
          user <- userStub
          roleWithId <- client.roles.create(role)
          objWithId <- service(client).create(obj)
          userWithId <- client.users.create(user)
          isIdempotent <- roleService(client).roles.users.assign(objWithId.id, userWithId.id, roleWithId.id).valueShouldIdempotentlyBe(())
        } yield isIdempotent
      }

      s"delete user role assignment" in {
        for {
          client <- scopedClient
          obj <- stub
          role <- roleStub
          user <- userStub
          roleWithId <- client.roles.create(role)
          objWithId <- service(client).create(obj)
          userWithId <- client.users.create(user)
          isIdempotent <- roleService(client).roles.users.delete(objWithId.id, userWithId.id, roleWithId.id).valueShouldIdempotentlyBe(())
        } yield isIdempotent
      }
      //#endregion
    }
}