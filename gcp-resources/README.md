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
| k8s.pod.name            |                                              | downward API or auto                                   |                                          |                                                |                                                |
| k8s.namespace.name      |                                              | downward API                                           |                                          |                                                |                                                |
| k8s.container.name      |                                              | hardcoded (manual)                                     |                                          |                                                |                                                |
| k8s.cluster.name        |                                              | auto                                                   |                                          |                                                |                                                |
| faas.name               |                                              |                                                        | auto                                     | auto                                           | auto                                           |
| faas.version            |                                              |                                                        | auto                                     | auto                                           | auto                                           |
| faas.instance           |                                              |                                                        | auto                                     | auto                                           | auto                                           |

## Downward API

For GKE applications, some values must be passed via the environment variable using k8s
"downward API".  For example, the following spec will ensure `k8s.namespace.name` and
`k8s.pod.name` are correctly discovered:

```yaml
spec:
  containers:
    - name: my-application
      image: gcr.io/my-project/my-image:latest
      env:
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: CONTAINER_NAME
          value: my-application
```

Additionally, the container name will only be discovered via the environment variable `CONTAINER_NAME`
which much be included in the environment.

## Usage with Manual Instrumentation

It is recommended to use this resource detector with the [OpenTelemetry Autoconfiguration SPI](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#resource-provider-spi). The GCP resource detector automatically provides the detected resources via the [autoconfigure-spi](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure-spi) SDK extension.

For a reference example showcasing the detected resource attributes and usage with `autoconfigure-spi`, see the [Resource detection example](https://github.com/open-telemetry/opentelemetry-java-examples/tree/main/resource-detection-gcp).

## Usage with Auto-Instrumentation

With the release of [v2.2.0 of the OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v2.2.0), the GCP resource detector is now included with the Java agent.

For users of Java Agent v2.2.0 and later, the GCP resource detectors can be enabled by following the instructions provided [here](https://opentelemetry.io/docs/languages/java/automatic/configuration/#enable-resource-providers-that-are-disabled-by-default).

## Component Owners

- [Josh Suereth](https://github.com/jsuereth), Google
- [Pranav Sharma](https://github.com/psx95), Google

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
