package pt.tecnico.dsi.openstack.keystone.models

import io.circe.derivation.Configuration

given Configuration = Configuration.default.withDefaults.withSnakeCaseMemberNames
