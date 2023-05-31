package pt.tecnico.dsi.openstack.keystone.models

import cats.derived.derived
import cats.derived.ShowPretty
import io.circe.derivation.{ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder, HCursor}

object CatalogEntry:
  private case class Url(id: String, interface: Interface, regionId: String, url: String) derives ConfiguredDecoder
  
  given decoder: Decoder[CatalogEntry] = (c: HCursor) => for
    tpe <- c.get[String]("type")
    serviceId <- c.get[String]("id")
    name <- c.get[String]("name")
    urls <- c.get[List[Url]]("endpoints")
  yield CatalogEntry(tpe, serviceId, name, urls.map(url => Endpoint(url.id, url.interface, url.regionId, url.url, serviceId)))
final case class CatalogEntry(
  `type`: String,
  serviceId: String,
  serviceName: String,
  endpoints: List[Endpoint],
) derives ConfiguredEncoder, ShowPretty {
  lazy val urisPerInterfacePerRegion: Map[Interface, Map[String, String]] = endpoints.groupBy(_.interface).view.mapValues { perInterface =>
    perInterface.groupMap(_.regionId)(_.url).view.mapValues(_.head).toMap
  }.toMap
  def urlsOf(interface: Interface): Option[Map[String, String]] = urisPerInterfacePerRegion.get(interface)
  def urlOf(interface: Interface, region: String): Option[String] = urlsOf(interface).flatMap(_.get(region))
}