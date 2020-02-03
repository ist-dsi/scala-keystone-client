package pt.tecnico.dsi.keystone.services

import cats.effect.Sync
import org.http4s._
import org.http4s.client.Client
import pt.tecnico.dsi.keystone.models.projects.Project

class ProjectService[F[_]: Sync]
  (uri: Uri, token: Header)
  (implicit client: Client[F]) extends CRUDService[F, Project] (uri, "project", token) {

  /**
    * TODO: get by name, and get by name and domain_id
    */

}
