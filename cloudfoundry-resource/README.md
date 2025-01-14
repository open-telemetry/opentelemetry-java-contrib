# OpenTelemetry CloudFoundry Resource Support

This module contains CloudFoundry resource detectors for OpenTelemetry.

The module detects environment variable `VCAP_APPLICATION`, which is present for applications deployed in CloudFoundry.
This variable contains a JSON structure, which is parsed to fill the following attributes.

| Resource attribute           | `VCAP_APPLICATION` field |
|------------------------------|--------------------------|
| cloudfoundry.app.id          | application_id           |
| cloudfoundry.app.name        | application_name         |
| cloudfoundry.app.instance.id | instance_index           |
| cloudfoundry.org.id          | organization_id          |
| cloudfoundry.org.name        | organization_name        |
| cloudfoundry.process.id      | process_id               |
| cloudfoundry.process.type    | process_type             |
| cloudfoundry.space.id        | space_id                 |
| cloudfoundry.space.name      | space_name               |

The resource attributes follow the [CloudFoundry semantic convention.](https://github.com/open-telemetry/semantic-conventions/blob/05b4c173bfdee2e972d252d14593b9fb653cc54a/docs/attributes-registry/cloudfoundry.md).
A description of `VCAP_APPLICATION` is available in the [CloudFoundry documentation](https://docs.cloudfoundry.org/devguide/deploy-apps/environment-variable.html#VCAP-APPLICATION).

## Component owners

- [Karsten Schnitter](https://github.com/KarstenSchnitter), SAP

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
