# Runtime attachment

If you can't update the JVM arguments to attach the [OpenTelemetry Java agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation) (_-javaagent:path/to/opentelemetry-javaagent.jar_), this project allows you to do attachment programmatically.

The `io.opentelemetry.contrib.attach.RuntimeAttach` class has an `attachJavaagentToCurrentJVM` method allowing to trigger the attachment of the OTel agent for Java.

The attachment will not be initiated in the following cases:
* The `otel.javaagent.enabled` property is set to `false`
* The `OTEL_JAVAAGENT_ENABLED` environment variable is set to `false`
* The attachment is not requested from the _main_ thread
* The agent is already attached

_The attachment must be requested at the beginning of the `main` method._ We give below an example for Spring Boot applications:

```java
@SpringBootApplication
public class SpringBootApp {

    public static void main(String[] args) {
        RuntimeAttach.attachJavaagentToCurrentJVM();
        SpringApplication.run(SpringBootApp.class, args);
    }

}
```

## Component owners

- [Nikita Salnikov-Tarnovski](https://github.com/iNikem), Splunk
- [Trask Stalnaker](https://github.com/trask), Microsoft

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
