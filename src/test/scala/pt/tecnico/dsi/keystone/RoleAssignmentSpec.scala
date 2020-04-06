package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.keystone.services.{CRUDService, RoleAssignmentService, RoleAssignment}

trait RoleAssignmentSpec[T] { this: CRUDSpec[T] =>

  def roleService: KeystoneClient[IO] => RoleAssignment[IO]

  def test[R](assignToStub: R, assignToCrud: CRUDService[IO, R], roleAssignmentService: RoleAssignmentService[IO]): Unit = {
    def createStubs: IO[(String, String, String)] = for {
      client <- scopedClient
      objStub <- stub
      obj <- service(client).create(objStub)
      stub <- assignToCrud.create(assignToStub)
      role <- client.roles.create(roleStub)
    } yield (obj.id, stub.id, role.id)

    s"list roles for a ${assignToCrud.name} in a ${name}" in {
      createStubs.flatMap { case (objId, stubId, _) =>
        roleAssignmentService.list(objId, stubId).compile.toList.map(_.length).valueShouldIdempotentlyBe(0)
      }
    }

    s"check ${assignToCrud.name} role assignment" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        roleAssignmentService.check(objId, stubId, roleId).valueShouldIdempotentlyBe(false)
      }
    }

    s"assign role to a ${assignToCrud.name}" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        roleAssignmentService.assign(objId, stubId, roleId).valueShouldIdempotentlyBe(())
      }
    }

    s"delete ${assignToCrud.name} role assignment" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        roleAssignmentService.delete(objId, stubId, roleId).valueShouldIdempotentlyBe(())
      }
    }
  }

  def groupStub = Group(
    name = "test-group",
    description = "test-desc",
    domainId = "default"
  )

  def userStub = User(
    name = "test-user",
    domainId = "default"
  )

  def roleStub = Role(
    name = "test-role2",
    description = Some("some-description"),
    domainId = None, // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
  )

  s"The ${name} service should handle role assignment" should {
    scopedClient.map { client =>
      val service = roleService(client)
      behave like test(groupStub, client.groups, service.roles.groups)
      behave like test(userStub, client.users, service.roles.users)
    }.unsafeRunSync()
  }
}