package pt.tecnico.dsi.openstack.keystone

import pt.tecnico.dsi.openstack.keystone.models.Scope

package object services {
  private[keystone] def domainIdFromScope(scope: Scope) = {
    // If the domain ID is not provided in the request, the Identity service will attempt to pull the domain ID from the token used in the request.
    // Note that this requires the use of a domain-scoped token.
    scope match {
      case Scope.Domain(id, _) => id
      case _ =>
        // https://github.com/openstack/keystone/blob/04316beecc0d20290fb36e7791eb3050953c1011/keystone/server/flask/common.py#L965
        // Not exactly the right value. Should be the default_domain_id from the keystone configuration, which we do not have access to.
        // According to the link above this logic will be removed. So we shouldn't include it, however if we do not creating Users, Groups and Projects
        // is no longer an idempotent operation :(
        "default"
    }
  }
}
