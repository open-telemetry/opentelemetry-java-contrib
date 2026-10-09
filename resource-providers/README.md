
# Resource Providers

[![Maven](https://img.shields.io/maven-central/v/io.opentelemetry.contrib/opentelemetry-resource-providers?label=Maven&color=orange)](https://central.sonatype.com/artifact/io.opentelemetry.contrib/opentelemetry-resource-providers)

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

## Usage with declarative configuration

You can configure the app server resource detector using declarative YAML configuration with the
OpenTelemetry SDK. For example:

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

The `app_server` detector needs to be listed before the `service` detector to allow overriding
the `service.name` attribute with the `OTEL_SERVICE_NAME` environment variable. Also, the resource
detectors in `detection/development` have lower priority over the explicit `resource.attributes`
configuration.

## Component owners

* [Jason Plumb](https://github.com/breedx-splk), Splunk
* [Lauri Tulmin](https://github.com/laurit), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
