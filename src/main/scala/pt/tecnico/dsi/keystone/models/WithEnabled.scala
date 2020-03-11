package pt.tecnico.dsi.keystone.models

trait WithEnabled[T] {
  def withEnabled(enabled: Boolean): T
  def enabled: Boolean
}
