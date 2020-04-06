package pt.tecnico.dsi.keystone

import cats.effect.IO
import pt.tecnico.dsi.keystone.models.{Group, Role, User}
import pt.tecnico.dsi.keystone.services.{CRUDService, GenericRoleAssignmentService, RoleAssignment}

trait RoleAssignmentSpec[F] {
  this: CRUDSpec[F] =>

  def roleService: KeystoneClient[IO] => RoleAssignment[IO]
  def test[T](stubIO: IO[T], crud: CRUDService[IO, T], roleAssignmentService: GenericRoleAssignmentService[IO]): Unit = {
      s"list roles for a ${crud.name} in a ${name}" in {
        for {
          client <- scopedClient
          obj <- stub
          stub <- stubIO
          objWithId <- service(client).create(obj)
          stubWithId <- crud.create(stub)
          isIdempotent <- roleAssignmentService.list(objWithId.id, stubWithId.id).compile.toList.map(_.length).valueShouldIdempotentlyBe(0)
        } yield isIdempotent
      }

      s"check ${crud.name} role assignment" in {
        for {
          client <- scopedClient
          obj <- stub
          role <- roleStub
          stub <- stubIO
          roleWithId <- client.roles.create(role)
          objWithId <- service(client).create(obj)
          stubWithId <- crud.create(stub)
          isIdempotent <- roleAssignmentService.check(objWithId.id, stubWithId.id, roleWithId.id).valueShouldIdempotentlyBe(false)
        } yield isIdempotent
      }

      s"assign role to a ${crud.name}" in {
        for {
          client <- scopedClient
          obj <- stub
          role <- roleStub
          stub <- stubIO
          roleWithId <- client.roles.create(role)
          objWithId <- service(client).create(obj)
          stubWithId <- crud.create(stub)
          isIdempotent <- roleAssignmentService.assign(objWithId.id, stubWithId.id, roleWithId.id).valueShouldIdempotentlyBe(())
        } yield isIdempotent
      }

      s"delete ${crud.name} role assignment" in {
        for {
          client <- scopedClient
          obj <- stub
          role <- roleStub
          stub <- stubIO
          roleWithId <- client.roles.create(role)
          objWithId <- service(client).create(obj)
          stubWithId <- crud.create(stub)
          isIdempotent <- roleService(client).roles.groups.delete(objWithId.id, stubWithId.id, roleWithId.id).valueShouldIdempotentlyBe(())
        } yield isIdempotent
      }

  }

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

  s"The ${name} service should handle role assignment" should {
    scopedClient.map { client =>
      val service = roleService(client)
      behave like test(groupStub, client.groups, service.roles.groups)
      behave like test(userStub, client.users, service.roles.users)
    }.unsafeRunSync()
  }
}