/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.kafka.impl;

import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.ByteBufferOutputStream;
import org.apache.kafka.common.utils.Bytes;

/**
 * An exporter of a messages encoded by {@link Marshaler} using the gRPC wire format.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class KafkaSender<T extends Marshaler> {

  private static final Logger logger = Logger.getLogger(KafkaSender.class.getName());

  private final String topic;

  private final long timeout;

  private final KafkaProducer<String, Bytes> kafkaProducer;

  public KafkaSender(String brokers, String topic, long timeout) {
    this.topic = topic;
    this.timeout = timeout;
    Properties properties = new Properties();
    properties.setProperty("bootstrap.servers", brokers);
    properties.setProperty("request.timeout.ms", "" + this.timeout);
    this.kafkaProducer =
        new KafkaProducer<String, Bytes>(properties, new StringSerializer(), new BytesSerializer());
  }

  public void send(T request, Runnable onSuccess, BiConsumer<RecordMetadata, Throwable> onFailure) {
    try {
      byte[] byteArray = new byte[request.getBinarySerializedSize()];
      ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
      OutputStream outputStream = new ByteBufferOutputStream(byteBuffer);
      request.writeBinaryTo(outputStream);
      // TODO
      ProducerRecord<String, Bytes> producerRecord =
          new ProducerRecord<String, Bytes>(topic, null, Bytes.wrap(byteArray));
      Callback callback =
          new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
              if (exception != null) {
                if (onFailure != null) {
                  try {
                    onFailure.accept(recordMetadata, exception);
                  } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "onFailure failed.", e);
                  }
                }
              } else {
                if (onSuccess != null) {
                  try {
                    onSuccess.run();
                  } catch (RuntimeException e) {
                    logger.log(Level.WARNING, "onSuccess failed.", e);
                  }
                }
              }
            }
          };
      Future<RecordMetadata> future = kafkaProducer.send(producerRecord, callback);
      if (future == null) {
        logger.log(Level.WARNING, "send to kafka failed.");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "send to kafka failed.", e);
    }
  }

  /** Shutdown the sender. */
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }
}
