# Runtime attachment

If you can't update the JVM arguments to attach the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation) (_-javaagent:path/to/opentelemetry-javaagent.jar_), this project allows you to do attachment programmatically.

## Quick start

### Add a dependency

Replace `OPENTELEMETRY_CONTRIB_VERSION` with the latest release version.

For Maven, add to your `pom.xml` the following dependency:

```xml
<dependency>
  <groupId>io.opentelemetry.contrib</groupId>
  <artifactId>opentelemetry-runtime-attach</artifactId>
  <version>OPENTELEMETRY_CONTRIB_VERSION</version>
</dependency>
```

For Gradle, add to your dependencies:

```groovy
implementation("io.opentelemetry.contrib:opentelemetry-runtime-attach:OPENTELEMETRY_CONTRIB_VERSION")
```

This dependency embeds the OpenTelemetry agent JAR.

### Call runtime attach method

The `io.opentelemetry.contrib.attach.RuntimeAttach` class has an `attachJavaagentToCurrentJVM` method allowing to trigger the attachment of the OTel agent for Java.

You have to call this method at the beginning of your application's `main` method.

We give below an example for Spring Boot applications:

```java
@SpringBootApplication
public class SpringBootApp {

    public static void main(String[] args) {
        RuntimeAttach.attachJavaagentToCurrentJVM();
        SpringApplication.run(SpringBootApp.class, args);
    }

}
```

## Limitations

The attachment will _not_ be initiated in the following cases:
* The `otel.javaagent.enabled` property is set to `false`
* The `OTEL_JAVAAGENT_ENABLED` environment variable is set to `false`
* The attachment is not requested from the _main_ thread
* The attachment is not requested from the `public static void main(String[] args)` method
* The agent is already attached
* The application is running on a JRE (a JDK is necessary, see [this issue](https://github.com/raphw/byte-buddy/issues/374))
* The temporary directory should be writable


## Component owners

- [Jean Bisutti](https://github.com/jeanbisutti), Microsoft
- [Nikita Salnikov-Tarnovski](https://github.com/iNikem), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
