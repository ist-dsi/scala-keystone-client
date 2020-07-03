# scala-keystone-client [![license](http://img.shields.io/:license-MIT-blue.svg)](LICENSE)
[![Scaladoc](http://javadoc-badge.appspot.com/pt.tecnico.dsi/scala-keystone-client_2.12.svg?label=scaladoc&style=plastic&maxAge=604800)](https://ist-dsi.github.io/scala-keystone-client/api/latest/pt/tecnico/dsi/openstack/keystone/index.html)
[![Latest version](https://index.scala-lang.org/ist-dsi/scala-keystone-client/scala-keystone-client/latest.svg)](https://index.scala-lang.org/ist-dsi/scala-keystone-client/scala-keystone-client)

[![Build Status](https://travis-ci.org/ist-dsi/scala-keystone-client.svg?branch=master&style=plastic&maxAge=604800)](https://travis-ci.org/ist-dsi/scala-keystone-client)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/1f752f2ed2d14ef5b301a55c772a32a4)](https://www.codacy.com/gh/ist-dsi/scala-keystone-client?utm_source=github.com&utm_medium=referral&utm_content=ist-dsi/scala-keystone-client&utm_campaign=Badge_Coverage)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/1f752f2ed2d14ef5b301a55c772a32a4)](https://www.codacy.com/gh/ist-dsi/scala-keystone-client?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ist-dsi/scala-keystone-client&amp;utm_campaign=Badge_Grade)
[![BCH compliance](https://bettercodehub.com/edge/badge/ist-dsi/scala-keystone-client)](https://bettercodehub.com/results/ist-dsi/scala-keystone-client)

A pure functional Scala client for Openstack Keystone implemented using Http4s client.

Supported endpoints:
- [Authentication and token management](https://docs.openstack.org/api-ref/identity/v3/#authentication-and-token-management)
  - Multi factor authentication is not implemented.
- [Credentials](https://docs.openstack.org/api-ref/identity/v3/#credentials)
- [Domains](https://docs.openstack.org/api-ref/identity/v3/#domains)
- [Groups](https://docs.openstack.org/api-ref/identity/v3/#groups)
- [Projects](https://docs.openstack.org/api-ref/identity/v3/#projects)
- [Regions](https://docs.openstack.org/api-ref/identity/v3/#regions)
- [Roles](https://docs.openstack.org/api-ref/identity/v3/#roles)
- [System Role Assignments](https://docs.openstack.org/api-ref/identity/v3/#system-role-assignments)
- [Service catalog and endpoints](https://docs.openstack.org/api-ref/identity/v3/#service-catalog-and-endpoints)  
- [Users](https://docs.openstack.org/api-ref/identity/v3/#users)

Unsupported endpoints (we accept PRs :)):
- [Application Credentials](https://docs.openstack.org/api-ref/identity/v3/#application-credentials)
- [Domain Configuration](https://docs.openstack.org/api-ref/identity/v3/#domain-configuration)
- [OS-INHERIT](https://docs.openstack.org/api-ref/identity/v3/#os-inherit)
- [Project Tags](https://docs.openstack.org/api-ref/identity/v3/#project-tags)
- [Unified Limits](https://docs.openstack.org/api-ref/identity/v3/#unified-limits)

[Latest scaladoc documentation](https://ist-dsi.github.io/scala-keystone-client/api/latest/pt/tecnico/dsi/openstack/keystone/index.html)

## Install
Add the following dependency to your `build.sbt`:
```sbt
libraryDependencies += "pt.tecnico.dsi" %% "scala-keystone-client" % "0.0.0"
```
We use [semantic versioning](http://semver.org).

## Usage
```scala
import scala.concurrent.ExecutionContext.Implicits.global
import cats.effect._
import org.http4s.client.blaze.BlazeClientBuilder
import pt.tecnico.dsi.openstack.keystone.KeystoneClient

object Example extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    BlazeClientBuilder[IO](global).resource.use { implicit httpClient =>
      for {
        client <- KeystoneClient.fromEnvironment()
        projects <- client.projects.list().compile.toList
        _ = println(projects.mkString("\n"))
      } yield ExitCode.Success
    }
  }
}
```

## License
scala-keystone-client is open source and available under the [MIT license](LICENSE).
