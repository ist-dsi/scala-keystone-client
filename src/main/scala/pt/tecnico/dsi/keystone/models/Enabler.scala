package pt.tecnico.dsi.keystone.models

trait Enabler[T] {
  def withEnabled(enabled: Boolean): T
}
