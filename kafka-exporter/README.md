# Exporting SpanData to Kafka

This module contains `KafkaSpanExporter`, which is an implementation of the `io.opentelemetry.sdk.trace.export.SpanExporter` interface.

`KafkaSpanExporter` can be used for sending `SpanData` to a Kafka topic.

## Usage

In order to instantiate a `KafkaSpanExporter`, you either need to pass a Kafka `Producer` or the configuration of a Kafka `Producer` together with key and value serializers.
You also need to pass the topic to which the SpanData need to be sent.
For a sample usage, see `KafkaSpanExporterIntegrationTest`.
