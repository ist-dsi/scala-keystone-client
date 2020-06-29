package pt.tecnico.dsi.keystone

import cats.effect.IO
import io.circe.Codec
import pt.tecnico.dsi.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.keystone.services.{CrudService, RoleAssignment, RoleAssignmentService}

trait RoleAssignmentSpec[T] { this: CrudSpec[T] =>
  def roleService: KeystoneClient[IO] => RoleAssignment[IO]

  def test[R: Codec](assignToStub: R, assignToCrud: CrudService[IO, R], roleAssignmentService: RoleAssignmentService[IO]): Unit = {
    def createStubs: IO[(String, String, String)] = {
      for {
        client <- scopedClient
        objStub <- stub
        obj <- service(client).create(objStub)
        stub <- assignToCrud.create(assignToStub)
        // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
        role <- client.roles.create(Role(s"role-assignment-$name", Some("some-description"), domainId = None))
      } yield (obj.id, stub.id, role.id)
    }

    s"list assigned roles to a ${assignToCrud.name}" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
          _ <- roleAssignmentService.assign(objId, stubId, roleId)
          idempotent <- roleAssignmentService.list(objId, stubId).compile.toList.idempotently(_.exists(_.id == roleId) shouldBe true)
        } yield idempotent
      }
    }

    s"assign role to a ${assignToCrud.name}" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
         _ <- roleAssignmentService.assign(objId, stubId, roleId).idempotently(_ shouldBe ())
         check <- roleAssignmentService.check(objId, stubId, roleId)
        } yield check shouldBe true
      }
    }

    s"delete ${assignToCrud.name} role assignment" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
          _ <- roleAssignmentService.delete(objId, stubId, roleId).idempotently(_ shouldBe ())
          check <- roleAssignmentService.check(objId, stubId, roleId)
        } yield check shouldBe false
      }
    }

    s"check ${assignToCrud.name} for role assignment" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
          _ <- roleAssignmentService.assign(objId, stubId, roleId)
          idempotent <- roleAssignmentService.check(objId, stubId, roleId).idempotently(_ shouldBe true)
        } yield idempotent
      }
    }

    s"check ${assignToCrud.name} for no role assignment" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
          _ <- roleAssignmentService.delete(objId, stubId, roleId)
          idempotent <- roleAssignmentService.check(objId, stubId, roleId).idempotently(_ shouldBe false)
        } yield idempotent
      }
    }
  }

  s"The ${name} service should handle role assignment" should {
    scopedClient.map { client =>
      val service = roleService(client)
      behave like test(Group("test-group", "description", domainId = "default"), client.groups, service.roles.groups)
      behave like test(User("test-user", "default"), client.users, service.roles.users)
    }.unsafeRunSync()
  }
}