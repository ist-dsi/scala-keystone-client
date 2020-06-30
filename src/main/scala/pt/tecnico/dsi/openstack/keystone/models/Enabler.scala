package pt.tecnico.dsi.openstack.keystone.models

trait Enabler[T] {
  def enabled: Boolean
  def withEnabled(enabled: Boolean): T
}
