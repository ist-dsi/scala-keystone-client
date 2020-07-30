package pt.tecnico.dsi.openstack.keystone

import cats.effect.{IO, Resource}
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.openstack.keystone.services.{RoleAssignment, RoleAssignmentService}

trait RoleAssignmentSpec[T <: Identifiable] { this: CrudSpec[T, _, _] =>
  def roleService: RoleAssignment[IO]

  def test[R <: Identifiable](assignTo: String, assignToResource: Resource[IO, R], roleAssignmentService: RoleAssignmentService[IO]): Unit = {
    val withStubs: Resource[IO, (String, String, String)] = {
      for {
        obj <- resource
        stub <- assignToResource
        // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
        role <- resourceCreator(keystone.roles)(n => Role.Create(s"role-assignment-$n"))
      } yield (obj.id, stub.id, role.id)
    }

    s"list assigned roles to a $assignTo" in withStubs.use[IO, Assertion] { case (objId, stubId, roleId) =>
      for {
        _ <- roleAssignmentService.assign(objId, stubId, roleId)
        idempotent <- roleAssignmentService.list(objId, stubId).compile.toList.idempotently(_.exists(_.id == roleId) shouldBe true)
      } yield idempotent
    }

    s"assign role to a $assignTo" in withStubs.use[IO, Assertion] { case (objId, stubId, roleId) =>
      for {
       _ <- roleAssignmentService.assign(objId, stubId, roleId).idempotently(_ shouldBe ())
       check <- roleAssignmentService.check(objId, stubId, roleId)
      } yield check shouldBe true
    }

    s"delete $assignTo role assignment" in withStubs.use[IO, Assertion] { case (objId, stubId, roleId) =>
      for {
        _ <- roleAssignmentService.delete(objId, stubId, roleId).idempotently(_ shouldBe ())
        check <- roleAssignmentService.check(objId, stubId, roleId)
      } yield check shouldBe false
    }

    s"check $assignTo for role assignment" in withStubs.use[IO, Assertion] { case (objId, stubId, roleId) =>
      for {
        _ <- roleAssignmentService.assign(objId, stubId, roleId)
        idempotent <- roleAssignmentService.check(objId, stubId, roleId).idempotently(_ shouldBe true)
      } yield idempotent
    }

    s"check $assignTo for no role assignment" in withStubs.use[IO, Assertion] { case (objId, stubId, roleId) =>
      for {
        _ <- roleAssignmentService.delete(objId, stubId, roleId)
        idempotent <- roleAssignmentService.check(objId, stubId, roleId).idempotently(_ shouldBe false)
      } yield idempotent
    }
  }

  s"The $name service should handle role assignment" should {
    behave like test("group", resourceCreator(keystone.groups)(Group.Create(_)), roleService.roles.groups)
    behave like test("user", resourceCreator(keystone.users)(User.Create(_, Some(randomName()))), roleService.roles.users)
  }
}