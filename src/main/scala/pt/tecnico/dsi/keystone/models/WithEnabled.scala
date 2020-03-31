package pt.tecnico.dsi.keystone.models

trait WithEnabled[T] {
  def enabled: Boolean
  def withEnabled(enabled: Boolean): T
}
