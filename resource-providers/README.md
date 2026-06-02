
# Resource Providers

[![Maven](https://badges.mvnrepository.com/badge/io.opentelemetry.contrib/opentelemetry-resource-providers/badge.svg?label=Maven&color=orange)](https://mvnrepository.com/artifact/io.opentelemetry.contrib/opentelemetry-resource-providers)

This module contains various `ResourceProvider` implementations.

## AppServerServiceNameProvider

This `ResourceProvider` will delegate to a collection of helpers that attempt
to populate the `service.name` resource attribute based on the runtime configuration
of an app server. This is useful when a user has not yet specified the `service.name`
resource attribute manually.

This `ResourceProvider` supports `.ear` and `.war` archives as well as exploded directory
versions of each. For `.war` files, it attempts to parse the `<web-app>` element
from `WEB-INF/web.xml`. For `.ear` files the `<application>` element of `META-INF/application.xml`.

It is capable of detecting common scenarios among the popular application servers listed below:

* Apache Tomcat
* Apache TomEE
* Eclipse Jetty
* GlassFish
* IBM Websphere
* IBM Websphere Liberty
* Wildfly

## Usage with Declarative configuration

```yaml
file_format: "1.0"
resource:
  detection/development:
    detectors:
      # Provides 'service.name' from the application server
      - app_server:
      # Provides 'service.name' and 'service.instance.id'
      - service:
```

The `app_server` detector needs to be listed before the `service` detector to give priority over
the `service.name` value set by the `service` detector. Also, the `detection/development` resource
detection has lower priority than the explicit configuration of `service.name` resource attribute.

## Component owners

* [Jason Plumb](https://github.com/breedx-splk), Splunk
* [Lauri Tulmin](https://github.com/laurit), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
