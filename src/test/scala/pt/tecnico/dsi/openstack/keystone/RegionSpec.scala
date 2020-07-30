package pt.tecnico.dsi.openstack.keystone

import cats.effect.IO
import org.scalatest.Assertion
import pt.tecnico.dsi.openstack.keystone.models.Region
import pt.tecnico.dsi.openstack.keystone.models.Region.Update
import pt.tecnico.dsi.openstack.keystone.services.Regions

class RegionSpec extends CrudSpec[Region, Region.Create, Region.Update]("region") {
  override def service: Regions[IO] = keystone.regions

  override def createStub(name: String): Region.Create = Region.Create(Some(name), Some("a description"))
  override def compareCreate(create: Region.Create, model: Region): Assertion = {
    model.id shouldBe create.id.value
    model.description shouldBe create.description
    model.parentRegionId.isEmpty shouldBe true
  }

  override def updateStub: Update = Region.Update(description = Some("a better and improved description"))
  override def compareUpdate(update: Update, model: Region): Assertion = {
    model.description shouldBe update.description
  }
}
