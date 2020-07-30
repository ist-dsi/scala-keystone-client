package pt.tecnico.dsi.openstack.keystone.services

import pt.tecnico.dsi.openstack.common.models.Identifiable
import pt.tecnico.dsi.openstack.common.services.CrudService

trait EnableDisableEndpoints[F[_], T <: Identifiable] { self: CrudService[F, T, _, _] =>
  protected def updateEnable(id: String, enabled: Boolean) : F[T]

  def disable(id: String): F[T] = updateEnable(id, enabled = false)
  def enable(id: String): F[T] = updateEnable(id, enabled = true)
}
