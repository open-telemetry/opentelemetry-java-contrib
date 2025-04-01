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

| Messaging system  | Version        | Wrapper type |
|-------------------|----------------|--------------|
| Aliyun mns-client | 1.3.0-SNAPSHOT | process      |

## Component owners

- [Minghui Zhang](https://github.com/Cirilla-zmh), Alibaba Cloud
- [Steve Rao](https://github.com/steverao), Alibaba Cloud

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
