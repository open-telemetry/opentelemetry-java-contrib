# GCP Resource Detectors for OpenTelemetry

This module provides GCP resource detectors for OpenTelemetry.

The following OpenTelemetry semantic conventions will be detected:

| Resource attribute      | [GCE](https://cloud.google.com/compute/docs) | [GKE](https://cloud.google.com/kubernetes-engine/docs) | [GCR](https://cloud.google.com/run/docs) | [GCF](https://cloud.google.com/functions/docs) | [GAE](https://cloud.google.com/appengine/docs) |
|-------------------------|----------------------------------------------|--------------------------------------------------------|------------------------------------------|------------------------------------------------|------------------------------------------------|
| cloud.platform          | gcp_compute_engine                           | gcp_kubernetes_engine                                  | gcp_cloud_run                            | gcp_cloud_functions                            | gcp_app_engine                                 |
| cloud.provider          | gcp                                          | gcp                                                    | gcp                                      | gcp                                            | gcp                                            |
| cloud.account.id        | auto                                         | auto                                                   | auto                                     | auto                                           | auto                                           |
| cloud.availability_zone | auto                                         | auto                                                   | auto                                     | auto                                           | auto                                           |
| cloud.region            | auto                                         | auto                                                   | auto                                     | auto                                           | auto                                           |
| host.id                 | auto                                         | auto                                                   |                                          |                                                |                                                |
| host.name               | auto                                         | auto                                                   |                                          |                                                |                                                |
| host.type               | auto                                         | auto                                                   |                                          |                                                |                                                |
| k8s.pod.name            |                                              | downward API                                           |                                          |                                                |                                                |
| k8s.namespace.name      |                                              | downward API                                           |                                          |                                                |                                                |
| k8s.container.name      |                                              | hardcoded (manual)                                     |                                          |                                                |                                                |
| k8s.cluster.name        |                                              | auto                                                   |                                          |                                                |                                                |
| faas.name               |                                              |                                                        | auto                                     | auto                                           | auto                                           |
| faas.version            |                                              |                                                        | auto                                     | auto                                           | auto                                           |
| faas.instance           |                                              |                                                        | auto                                     | auto                                           | auto                                           |

## Setting Kubernetes attributes

This resource detector does not detect the following resource attributes
`container.name`, `k8s.pod.name` and `k8s.namespace.name`.  When using this detector,
you should use this in your Pod Spec to set these using
[`OTEL_RESOURCE_ATTRIBUTES`](https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/resource/sdk.md#specifying-resource-information-via-an-environment-variable):

```yaml
env:
- name: POD_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: NAMESPACE_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
- name: CONTAINER_NAME
  value: my-container-name
- name: OTEL_RESOURCE_ATTRIBUTES
  value: k8s.pod.name=$(POD_NAME),k8s.namespace.name=$(NAMESPACE_NAME),k8s.container.name=$(CONTAINER_NAME)
```

## Usage with Manual Instrumentation

It is recommended to use this resource detector with the [OpenTelemetry SDK autoconfiguration](https://opentelemetry.io/docs/languages/java/configuration/#zero-code-sdk-autoconfigure). The GCP resource detector automatically provides the detected resources via the [autoconfigure-spi](https://opentelemetry.io/docs/languages/java/configuration/#spi-service-provider-interface) SDK extension.

For a reference example showcasing the detected resource attributes and usage with `autoconfigure-spi`, see the [Resource detection example](https://github.com/open-telemetry/opentelemetry-java-examples/tree/main/resource-detection-gcp).

## Usage with Auto-Instrumentation

With the release of [v2.2.0 of the OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.2.0), the GCP resource detector is now included with the Java agent.

For users of Java Agent v2.2.0 and later, the GCP resource detectors can be enabled by following the instructions provided in the [agent configuration documentation](https://opentelemetry.io/docs/languages/java/automatic/configuration/#enable-resource-providers-that-are-disabled-by-default).

## Component Owners

- [Josh Suereth](https://github.com/jsuereth), Google
- [Pranav Sharma](https://github.com/psx95), Google

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
