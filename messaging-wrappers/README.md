# OpenTelemetry Messaging Wrappers

This is a lightweight messaging wrappers API designed to help you quickly add instrumentation to any
type of messaging system client. To further ease the burden of instrumentation, we will also provide
predefined implementations for certain messaging systems, helping you seamlessly address the issue
of broken traces.

<details>
<summary>Table of Contents</summary>

- [Overview](#overview)
- [Predefined Implementations](#predefined-implementations)
- [Quickstart For Given Implementations](#quickstart-for-given-implementations)
  - [\[Given\] Step 1 Add dependencies](#given-step-1-add-dependencies)
  - [\[Given\] Step 2 Initializing MessagingWrappers](#given-step-2-initializing-messagingwrappers)
  - [\[Given\] Step 3 Wrapping the Process](#given-step-3-wrapping-the-process)
- [Manual Implementation](#manual-implementation)
  - [\[Manual\] Step 1 Add dependencies](#manual-step-1-add-dependencies)
  - [\[Manual\] Step 2 Initializing MessagingWrappers](#manual-step-2-initializing-messagingwrappers)
  - [\[Manual\] Step 3 Wrapping the Process](#manual-step-3-wrapping-the-process)
- [Component Owners](#component-owners)

</details>

## Overview

The primary goal of this API is to simplify the process of adding instrumentation to your messaging
systems, thereby enhancing observability without introducing significant overhead. Inspired by
[#13340](https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/13340) and
[opentelemetry-java-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/instrumentation-api-incubator/src/main/java/io/opentelemetry/instrumentation/api/incubator/semconv/messaging/MessagingAttributesExtractor.java),
this tool aims to streamline the tracing and monitoring process.

## Predefined Implementations

| Messaging system  | Version Scope | Wrapper type |
|-------------------|---------------|--------------|
| kafka-clients     | `[0.11.0.0,)` | process      |
| aliyun mns-client | `[1.3.0,)`    | process      |

## Quickstart For Given Implementations

This example will demonstrate how to add automatic instrumentation to your Kafka consumer with process wrapper. For
detailed example, please check out [KafkaClientTest](./kafka-clients/src/test/java/io/opentelemetry/contrib/messaging/wrappers/kafka/KafkaClientTest.java).

### [Given] Step 1 Add dependencies

To use OpenTelemetry in your project, you need to add the necessary dependencies. Below are the configurations for both
Gradle and Maven.

#### Gradle

```kotlin
dependencies {
    implementation("io.opentelemetry.contrib:opentelemetry-messaging-wrappers-kafka-clients:${latest_version}")
}
```

#### Maven

```xml
<dependency>
    <groupId>io.opentelemetry.contrib</groupId>
    <artifactId>opentelemetry-messaging-wrappers-kafka-clients</artifactId>
    <version>${latest_version}</version>
</dependency>
```

### [Given] Step 2 Initializing MessagingWrappers

For `kafka-clients`, we provide pre-implemented wrappers that allow for out-of-the-box integration. We provide
an implementation based on the OpenTelemetry semantic convention by default.

```java
public class KafkaDemo {
 
  public static MessagingProcessWrapper<KafkaProcessRequest> createWrapper() {
    return KafkaHelper.processWrapperBuilder().build();
  }
}
```

### [Given] Step 3 Wrapping the Process

Once the MessagingWrapper are initialized, you can wrap your message processing logic to ensure that tracing spans are
properly created and propagated.

**P.S.** Some instrumentations may also [generate process spans](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).
If both are enabled, it might result in duplicate nested process spans. It is recommended to disable one of them.

```java
public class Demo {

  private static final MessagingProcessWrapper<KafkaProcessRequest> WRAPPER = createWrapper();

  // please initialize consumer
  private Consumer<Integer, String> consumer;
  
  public String consume() {
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5));
    ConsumerRecord<?, ?> record = records.iterator().next();

    return WRAPPER.doProcess(
        KafkaProcessRequest.of(record, groupId, clientId), () -> {
          // your processing logic
          return "success";
        });
  }

  public void consumeWithoutResult() {
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5));
    ConsumerRecord<?, ?> record = records.iterator().next();

    WRAPPER.doProcess(
        KafkaProcessRequest.of(record, groupId, clientId), () -> {
          // your processing logic
        });
  }
}
```

## Manual Implementation

You can also build implementations based on the `messaging-wrappers-api` for any messaging system to accommodate your
custom message protocol. For detailed example, please check out [UserDefinedMessageSystemTest](./api/src/test/java/io/opentelemetry/contrib/messaging/wrappers/UserDefinedMessageSystemTest.java).

### [Manual] Step 1 Add dependencies

#### Gradle

```kotlin
dependencies {
    implementation("io.opentelemetry.contrib:opentelemetry-messaging-wrappers-api:${latest_version}")
}
```

#### Maven

```xml
<dependency>
    <groupId>io.opentelemetry.contrib</groupId>
    <artifactId>opentelemetry-messaging-wrappers-api</artifactId>
    <version>${latest_version}</version>
</dependency>
```

### [Manual] Step 2 Initializing MessagingWrappers

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
You can also customize your messaging spans by adding an `AttributesExtractor`.

### [Manual] Step 3 Wrapping the Process

Once the MessagingWrapper are initialized, you can wrap your message processing logic to ensure that tracing spans are
properly created and propagated.

**P.S.** Some instrumentations may also [generate process spans](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/supported-libraries.md).
If both are enabled, it might result in duplicate nested process spans. It is recommended to disable one of them.

```java
public class Demo {
 
  private static final MessagingProcessWrapper<MyMessagingProcessRequest> WRAPPER = createWrapper();
 
  public String consume(Message message) {
    return WRAPPER.doProcess(new MyMessagingProcessRequest(message), () -> {
      // your processing logic
      return "success";
    });
  }

  public void consumeWithoutReturn(Message message) {
    WRAPPER.doProcess(new MyMessagingProcessRequest(message), () -> {
      // your processing logic
    });
  }
}
```

## Component Owners

- [Minghui Zhang](https://github.com/Cirilla-zmh), Alibaba
- [Steve Rao](https://github.com/steverao), Alibaba

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).

