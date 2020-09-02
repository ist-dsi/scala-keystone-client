package pt.tecnico.dsi.openstack.keystone.models

import pt.tecnico.dsi.openstack.keystone.KeystoneClient
import pt.tecnico.dsi.openstack.keystone.services.RoleAssignment

case object System extends RoleAssigner {
  def roleAssignment[F[_]](implicit client: KeystoneClient[F]): RoleAssignment[F] = client.roles.on(this)
}
