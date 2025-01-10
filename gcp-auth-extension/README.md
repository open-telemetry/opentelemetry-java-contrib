# Google Cloud Authentication Extension

The Google Cloud Auth Extension allows the users to export telemetry from their applications to Google Cloud using the built-in OTLP exporters.\
The extension takes care of the necessary configuration required to authenticate to GCP to successfully export telemetry.

## Prerequisites

### Ensure the presence of Google Cloud Credentials on your machine/environment

```shell
gcloud auth application-default login
```

Executing this command will save your application credentials to default path which will depend on the type of machine -

- Linux, macOS: `$HOME/.config/gcloud/application_default_credentials.json`
- Windows: `%APPDATA%\gcloud\application_default_credentials.json`

**NOTE: This method of authentication is not recommended for production environments.**

Next, export the credentials to `GOOGLE_APPLICATION_CREDENTIALS` environment variable -

For Linux & MacOS:

```shell
export GOOGLE_APPLICATION_CREDENTIALS=$HOME/.config/gcloud/application_default_credentials.json
```

These credentials are built-in running in a Google App Engine, Google Cloud Shell or Google Compute Engine environment.

### Configuring the extension

The extension can be configured either by environment variables or system properties.

Here is a list of configurable options for the extension:

- `GOOGLE_CLOUD_PROJECT`: Environment variable that represents the Google Cloud Project ID to which the telemetry needs to be exported.
  - Can also be configured using `google.cloud.project` system property.
  - If this option is not configured, the extension would infer GCP Project ID from the application default credentials. For more information on application default credentials, see [here](https://cloud.google.com/docs/authentication/application-default-credentials).

## Usage

### With OpenTelemetry Java agent

The OpenTelemetry Java Agent Extension can be easily added to any Java application by modifying the startup command to the application.
For more information on Extensions, see the [documentation here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md).

Below is a snippet showing how to add the extension to a Java application using the Gradle build system.

```gradle
// Specify OpenTelemetry Autoinstrumentation Java Agent Path.
def otelAgentPath = <OpenTelemetry Java Agent location>
// Specify the path for Google Cloud Authentication Extension for the Java Agent.
def extensionPath = <Google Cloud Authentication Extension location>
def googleCloudProjectId = <Your Google Cloud Project ID>
def googleOtlpEndpoint = <Google Cloud OTLP endpoint>

val autoconf_config = listOf(
  "-javaagent:${otelAgentPath}",
  "-Dotel.javaagent.extensions=${extensionPath}",
  // Configure the GCP Auth extension using system properties.
  // This can also be configured using environment variables.
  "-Dgoogle.cloud.project=${googleCloudProjectId}",
  // Configure auto instrumentation.
  "-Dotel.exporter.otlp.traces.endpoint=${googleOtlpEndpoint}",
  '-Dotel.java.global-autoconfigure.enabled=true',
  // Optionally enable the built-in GCP resource detector
  '-Dotel.resource.providers.gcp.enabled=true'
  '-Dotel.traces.exporter=otlp',
  '-Dotel.metrics.exporter=logging'
)

application {
  ...
  applicationDefaultJvmArgs = autoconf_config
  ...
}
```

### Without OpenTelemetry Java agent

This extension can be used without the OpenTelemetry Java agent by leveraging the [OpenTelemetry SDK Autoconfigure](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md) module.\
When using the autoconfigured SDK, simply adding this extension as a dependency automatically configures authentication headers and resource attributes for spans, enabling export to Google Cloud.

Below is a snippet showing how to use this extension as a dependency when the application is not instrumented using the OpenTelemetry Java agent.

```gradle
dependencies {
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    // include the auth extension dependency
    implementation("io.opentelemetry.contrib:opentelemetry-gcp-auth-extension")

    // other dependencies
    ...

}

val autoconf_config = listOf(
  '-Dgoogle.cloud.project=your-gcp-project-id',
  '-Dotel.exporter.otlp.endpoint=https://your.otlp.endpoint:1234',
  '-Dotel.traces.exporter=otlp',
  '-Dotel.java.global-autoconfigure.enabled=true'

  // any additional args
  ...
)

application {
  applicationDefaultJvmArgs = autoconf_config

  // additional configuration
  ...
}
```

## Component Owners

- [Josh Suereth](https://github.com/jsuereth), Google
- [Pranav Sharma](https://github.com/psx95), Google

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
