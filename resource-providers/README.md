
# Resource Providers

This module contains various `ResourceProvider` implementations.

## AppServerServiceNameProvider

This `ResourceProvider` will delegate to a collection of helpers that attempt
to populate the `service.name` resource attribute based on the runtime configuration
of an app server. This is useful when a user has not yet specified the `service.name`
resource attribute manually.

It is capable of detecting common scenarios among the following popular application servers:

* tbd (will be filled in as implementations are added)
