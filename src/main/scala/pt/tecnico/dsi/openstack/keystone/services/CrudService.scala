package pt.tecnico.dsi.openstack.keystone.services

import cats.effect.Sync
import io.circe.Codec
import org.http4s.client.Client
import org.http4s.{Header, Uri}
import pt.tecnico.dsi.openstack.common.services.{CrudService => CommonCrudService}

abstract class CrudService[F[_]: Sync: Client, Model: Codec](baseUri: Uri, name: String, authToken: Header)
  extends CommonCrudService[F, Model, Model, Model](baseUri, name, authToken)