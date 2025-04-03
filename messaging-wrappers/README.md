# OpenTelemetry Messaging Wrappers

This is a lightweight messaging wrappers API designed to help you quickly add instrumentation to any
type of messaging system client. To further ease the burden of instrumentation, we will also provide
predefined implementations for certain messaging systems, helping you seamlessly address the issue 
of broken traces.

## Overview

The primary goal of this API is to simplify the process of adding instrumentation to your messaging 
systems, thereby enhancing observability without introducing significant overhead. Inspired by 
[#13340](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/13340) and 
[opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/main/java/io/opentelemetry/instrumentation/api/incubator/semconv/messaging/MessagingAttributesExtractor.java), 
this tool aims to streamline the tracing and monitoring process.

## Predefined Implementations

| Messaging system  | Version Scope       | Wrapper type |
|-------------------|---------------------|--------------|
| kafka-clients     | `[0.11.0.0,)`       | process      |
| aliyun mns-client | `[1.3.0-SNAPSHOT,)` | process      |

## Quickstart

### Step 1 Add dependencies

To use OpenTelemetry in your project, you need to add the necessary dependencies. Below are the configurations for both
Gradle and Maven.

#### Gradle

```kotlin
dependencies {
    implementation("io.opentelemetry.contrib:opentelemetry-messaging-wrappers-api")
}
```

#### Maven

```xml
<dependency>
    <groupId>io.opentelemetry.contrib</groupId>
    <artifactId>opentelemetry-messaging-wrappers-api</artifactId>
</dependency>
```

### Step 2 Initializing MessagingWrappers

Below is an example of how to initialize a messaging wrapper.

```java
public class Demo {

  public static MessagingProcessWrapper<MyMessagingProcessRequest> createWrapper(
      OpenTelemetry openTelemetry,
      MyTextMapGetter textMapGetter,
      List<AttributesExtractor<MyMessagingProcessRequest, Void>> additionalExtractor) {

    return MessagingProcessWrapper.<MyMessagingProcessRequest>defaultBuilder()
        .openTelemetry(openTelemetry)
        .textMapGetter(textMapGetter)
        .addAttributesExtractors(additionalExtractor)
        .build();
  }
}

public class MyMessagingProcessRequest implements MessagingProcessRequest {
  // your implementation here
}

public class MyTextMapGetter implements TextMapGetter<MyMessagingProcessRequest> {
  // your implementation here
}
```

For arbitrary messaging systems, you need to manually define `MessagingProcessRequest` and the corresponding `TextMapGetter`.
You can also customize your messaging spans by adding an AttributesExtractor.

For popular messaging systems, we provide pre-implemented wrappers that allow for out-of-the-box integration. We provide
an implementation based on the OpenTelemetry semantic convention by default.

```java
public class KafkaDemo {
  
  public static MessagingProcessWrapper<KafkaProcessRequest> createWrapper() {
    return KafkaHelper.processWrapperBuilder().build();
  }
}
```

### Step 3 Wrapping your process

Once the MessagingWrapper are initialized, you can wrap your message processing logic to ensure that tracing spans are
properly created and propagated.

**P.S.** Some instrumentations may also [generate process spans](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).
If both are enabled, it might result in duplicate nested process spans. It is recommended to disable one of them.

```java
public class Demo {
  
  private static final MessagingProcessWrapper<MyMessagingProcessRequest> WRAPPER = createWrapper();
  
  public String consume(Message message) {
    WRAPPER.doProcess(new MyMessagingProcessRequest(message), () -> {
      // your processing logic
    });
  }
}
```

## Component owners

- [Minghui Zhang](https://github.com/Cirilla-zmh), Alibaba Cloud
- [Steve Rao](https://github.com/steverao), Alibaba Cloud

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
