package pt.tecnico.dsi.openstack.keystone

import cats.effect.{IO, Resource}
import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.openstack.keystone.services.RoleAssignment

trait RoleAssignmentSpec[T <: Identifiable] { this: CrudSpec[T, _, _] =>
  def roleService(model: T): RoleAssignment[IO]

  val withStubs: Resource[IO, (RoleAssignment[IO], User, Group, Role)] = {
    val fullResource = for {
      user <- resourceCreator(keystone.users)(User.Create(_, Some(randomName())))
      group <- resourceCreator(keystone.groups)(Group.Create(_))
      model <- resource
      // We cannot use Some(domainId) because listing roles by default does not list roles from all domains
      role <- resourceCreator(keystone.roles)(n => Role.Create(s"role-assignment-$n"))
    } yield (roleService(model), user, group, role)
    // Every test needs to assign the role
    fullResource.evalMap { case (roleService, user, group, role) =>
      for {
        _ <- roleService assign role to user
        _ <- roleService assign role to group
      } yield (roleService, user, group, role)
    }
  }
  
  s"The $name service should handle role assignment" should {
    "list assigned roles" in withStubs.use { case (roleService, user, group, role) =>
      for {
        _ <- roleService.listAssignmentsFor(user).idempotently(_.forall(_.id == role.id) shouldBe true)
        result <- roleService.listAssignmentsFor(group).idempotently(_.forall(_.id == role.id) shouldBe true)
      } yield result
    }
    "assign roles" in withStubs.use { case (roleService, user, group, role) =>
      for {
        _ <- (roleService assign role to user).idempotently(_ shouldBe ())
        _ <- (roleService assign role to group).idempotently(_ shouldBe ())
        checkUser <- roleService is role assignedTo user
        checkGroup <- roleService is role assignedTo group
      } yield {
        checkUser shouldBe true
        checkGroup shouldBe true
      }
    }
    "delete role assignments" in withStubs.use { case (roleService, user, group, role) =>
      for {
        _ <- (roleService unassign role from user).idempotently(_ shouldBe ())
        _ <- (roleService unassign role from group).idempotently(_ shouldBe ())
        checkUser <- roleService is role assignedTo user
        checkGroup <- roleService is role assignedTo group
      } yield {
        checkUser shouldBe false
        checkGroup shouldBe false
      }
    }
    "check role assignments" in withStubs.use { case (roleService, user, group, role) =>
      for {
        firstCheckUser <- (roleService is role assignedTo user).idempotently(_ shouldBe true)
        firstCheckGroup <- (roleService is role assignedTo group).idempotently(_ shouldBe true)
        _ <- roleService unassign role from user
        _ <- roleService unassign role from group
        secondCheckUser <- (roleService is role assignedTo user).idempotently(_ shouldBe false)
        secondCheckGroup <- (roleService is role assignedTo group).idempotently(_ shouldBe false)
      } yield {
        // Kinda redundant, I don't know how to implement this properly
        firstCheckUser shouldBe succeed
        firstCheckGroup shouldBe succeed
        secondCheckUser shouldBe succeed
        secondCheckGroup shouldBe succeed
      }
    }
  }
}