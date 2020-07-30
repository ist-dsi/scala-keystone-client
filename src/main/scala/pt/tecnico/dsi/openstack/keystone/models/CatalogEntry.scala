package pt.tecnico.dsi.openstack.keystone.models

import io.circe.derivation.{deriveDecoder, renaming}
import io.circe.{Decoder, HCursor}

object CatalogEntry {
  private implicit val decoderUrl: Decoder[Url] = deriveDecoder(renaming.snakeCase)
  private case class Url(id: String, interface: Interface, regionId: String, url: String)

  implicit val decoder: Decoder[CatalogEntry] = (c: HCursor) => for {
    tpe <- c.get[String]("type")
    serviceId <- c.get[String]("id")
    name <- c.get[String]("name")
    urls <- c.get[List[Url]]("endpoints")
  } yield CatalogEntry(tpe, serviceId, name, urls.map(url => Endpoint(url.id, url.interface, url.regionId, url.url, serviceId)))
}

case class CatalogEntry(`type`: String, serviceId: String, serviceName: String, endpoints: List[Endpoint]) {
  lazy val urisPerRegionPerInterface: Map[String, Map[Interface, String]] = endpoints.groupBy(_.regionId).view.mapValues { perRegion =>
    perRegion.groupMap(_.interface)(_.url).view.mapValues(_.head).toMap
  }.toMap
  def urlsOf(region: String): Option[Map[Interface, String]] = urisPerRegionPerInterface.get(region)
  def urlOf(region: String, interface: Interface): Option[String] = urlsOf(region).flatMap(_.get(interface))
}