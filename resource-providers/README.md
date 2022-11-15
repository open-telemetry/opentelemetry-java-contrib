
# Resource Providers

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

* GlassFish
* _remaining are tbd_

## Component owners

- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Mateusz Rzeszutek](https://github.com/mateuszrzeszutek), Splunk
- [Lauri Tulmin](https://github.com/laurit), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
