package pt.tecnico.dsi.keystone

import cats.effect.IO
import io.circe.Codec
import org.scalatest.BeforeAndAfterEach
import pt.tecnico.dsi.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.keystone.services.{CrudService, RoleAssignment, RoleAssignmentService}

trait RoleAssignmentSpec[T] extends BeforeAndAfterEach { this: CrudSpec[T] =>
  var roles = 0
  override def beforeEach(): Unit = {
    val cleanup = for {
      client <- scopedClient
      list <- service(client).list().compile.toList
    } yield list.foreach(s => service(client).delete(s.id))
    cleanup.unsafeRunSync()
  }

  def roleService: KeystoneClient[IO] => RoleAssignment[IO]

  def test[R: Codec](assignToStub: R, assignToCrud: CrudService[IO, R], roleAssignmentService: RoleAssignmentService[IO]): Unit = {
    def createStubs: IO[(String, String, String)] = {
      roles += 1
      for {
        client <- scopedClient
        objStub <- stub
        obj <- service(client).create(objStub)
        stub <- assignToCrud.create(assignToStub)
        // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
        role <- client.roles.create(Role(s"test-role#{$roles}", Some("some-description"), domainId = None))
      } yield (obj.id, stub.id, role.id)
    }

    s"list roles for a ${assignToCrud.name} in a ${name}" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        val roles = roleAssignmentService.list(objId, stubId).compile.toList
        for {
          _ <- roleAssignmentService.assign(objId, stubId, roleId)
          idempotent <- roles.idempotently(_.exists(_.id == roleId) shouldBe true)
        } yield idempotent
      }
    }

    s"check ${assignToCrud.name} for role assignment" in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
          _ <- roleAssignmentService.assign(objId, stubId, roleId)
          idempotent <- roleAssignmentService.check(objId, stubId, roleId).valueShouldIdempotentlyBe(true)
        } yield idempotent
      }
    }

    s"check ${assignToCrud.name} for no role assignment" in {
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

    s"check ${assignToCrud.name} role assignment after delete " in {
      createStubs.flatMap { case (objId, stubId, roleId) =>
        for {
          _ <- roleAssignmentService.delete(objId, stubId, roleId)
          check <- roleAssignmentService.check(objId, stubId, roleId).valueShouldBe(false)
        } yield check
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