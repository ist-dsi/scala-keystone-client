package pt.tecnico.dsi.keystone.models

import io.circe.derivation.{deriveDecoder, renaming}
import io.circe.{Decoder, HCursor}

object CatalogEntry {
  private implicit val decoderUrl: Decoder[Url] = deriveDecoder(renaming.snakeCase, false, None)
  private case class Url(id: String, interface: Interface, regionId: String, url: String)

  implicit val decoder: Decoder[CatalogEntry] = (c: HCursor) => for {
    `type` <- c.get[String]("type")
    serviceId <- c.get[String]("id")
    name <- c.get[String]("name")
    urls <- c.get[List[Url]]("endpoints")
  } yield CatalogEntry(`type`, serviceId, name, urls.map(url => WithId(url.id, Endpoint(url.interface, url.regionId, url.url, serviceId), None)))
}

case class CatalogEntry(`type`: String, serviceId: String, serviceName: String, endpoints: List[WithId[Endpoint]]) {
  lazy val urisPerRegionPerInterface: Map[String, Map[Interface, String]] = endpoints.groupBy(_.regionId).view.mapValues { perRegion =>
    perRegion.groupMap(_.interface)(_.url).view.mapValues(_.head).toMap
  }.toMap
  def urlOf(region: String, interface: Interface): Option[String] = urisPerRegionPerInterface.get(region).flatMap(_.get(interface))
  def urlsOf(region: String): Option[Map[Interface, String]] = urisPerRegionPerInterface.get(region)
}