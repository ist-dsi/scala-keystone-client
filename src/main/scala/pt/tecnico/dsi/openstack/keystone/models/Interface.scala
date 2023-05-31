package pt.tecnico.dsi.openstack.keystone.models

import cats.Show
import cats.derived.derived
import io.circe.derivation.{Configuration, ConfiguredEnumCodec}

object Interface:
  given Configuration = Configuration.default.withTransformConstructorNames(_.toLowerCase)
enum Interface derives ConfiguredEnumCodec, Show:
  case Public, Admin, Internal