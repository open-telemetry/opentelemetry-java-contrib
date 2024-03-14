# Azure Resource Detectors for OpenTelemetry

This module provides Azure resource detectors for OpenTelemetry.

The following OpenTelemetry semantic conventions will be detected:

| Resource attribute      | VM       | Functions       | App Service       | Containers           |
|-------------------------|----------|-----------------|-------------------|----------------------|
| cloud.platform          | azure_vm | azure_functions | azure_app_service | azure_container_apps |
| cloud.provider          | azure    | azure           | azure             | azure                |
| cloud.resource.id       | auto     |                 | auto              |                      |
| cloud.region            | auto     | auto            | auto              |                      |
| deployment.environment  |          |                 | auto              |                      |
| host.id                 | auto     |                 | auto              |                      |
| host.name               | auto     |                 |                   |                      |
| host.type               | auto     |                 |                   |                      |
| os.type                 | auto     |                 |                   |                      |
| os.version              | auto     |                 |                   |                      |
| azure.vm.scaleset.name  | auto     |                 |                   |                      |
| azure.vm.sku            | auto     |                 |                   |                      |
| service.name            |          |                 | auto              | auto                 |
| service.version         |          |                 |                   | auto                 |
| service.instance.id     |          |                 | auto              | auto                 |
| azure.app.service.stamp |          |                 | auto              |                      |
| faas.name               |          | auto            |                   |                      |
| faas.version            |          | auto            |                   |                      |
| faas.instance           |          | auto            |                   |                      |
| faas.faas.max_memory    |          | auto            |                   |                      |

## Component Owners

TODO

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
