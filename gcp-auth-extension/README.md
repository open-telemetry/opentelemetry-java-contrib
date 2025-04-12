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

Here is a list of required and optional configuration available for the extension:

#### Required Config

- `GOOGLE_CLOUD_PROJECT`: Environment variable that represents the Google Cloud Project ID to which the telemetry needs to be exported.

  - Can also be configured using `google.cloud.project` system property.
  - This is a required option, the agent configuration will fail if this option is not set.

#### Optional Config

- `GOOGLE_CLOUD_QUOTA_PROJECT`: Environment variable that represents the Google Cloud Quota Project ID which will be charged for the GCP API usage. To learn more about a *quota project*, see [here](https://cloud.google.com/docs/quotas/quota-project). Additional details about configuring the *quota project* can be found [here](https://cloud.google.com/docs/quotas/set-quota-project).

  - Can also be configured using `google.cloud.quota.project` system property.

## Usage

### With OpenTelemetry Java agent

The OpenTelemetry Java Agent Extension can be easily added to any Java application by modifying the startup command to the application.
For more information on Extensions, see the [documentation here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/examples/extension/README.md).

> [!IMPORTANT]
> Make sure to download the 'shaded' variant of the Authentication Extension for use with OpenTelemetry Java auto-instrumentation agent. The shaded version is available under the classifier name `shadow`.\
> See instructions for [Downloading Shaded JAR](#downloading-shaded-jar) below.

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

#### Downloading Shaded JAR

You can download the shaded JAR for Google Cloud Authentication Extension from the following link -

```text
https://repo1.maven.org/maven2/io/opentelemetry/contrib/opentelemetry-gcp-auth-extension/<VERSION>/opentelemetry-gcp-auth-extension-<VERSION>-shadow.jar
```

Replace `<VERSION>` with the version you wish to download. For instance, shaded
variant for `v1.44.0-alpha`, will be found at -

`https://repo1.maven.org/maven2/io/opentelemetry/contrib/opentelemetry-gcp-auth-extension/1.44.0-alpha/opentelemetry-gcp-auth-extension-1.44.0-alpha-shadow.jar`

*Note: Typically, you would want to use the most recent version of the extension.*

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
