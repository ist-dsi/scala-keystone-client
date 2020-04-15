package pt.tecnico.dsi.keystone.models

trait Enabler[T] {
  def enabled: Boolean
  def withEnabled(enabled: Boolean): T
}
