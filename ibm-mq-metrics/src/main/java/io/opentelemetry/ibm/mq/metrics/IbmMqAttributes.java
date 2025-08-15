/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.ibm.mq.metrics;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.common.AttributeKey.longKey;
import io.opentelemetry.api.common.AttributeKey;

// This file is generated using weaver. Do not edit manually.

/** Attribute definitions generated from a Weaver model. Do not edit manually. */
public final class IbmMqAttributes {

  /**
  The name of the IBM queue manager
  */
  public final static AttributeKey<String> IBM_MQ_QUEUE_MANAGER = stringKey("ibm.mq.queue.manager");
  

  /**
  The system-specific name of the messaging operation.
  */
  public final static AttributeKey<String> MESSAGING_DESTINATION_NAME = stringKey("messaging.destination.name");
  

  /**
  The name of the channel
  */
  public final static AttributeKey<String> IBM_MQ_CHANNEL_NAME = stringKey("ibm.mq.channel.name");
  

  /**
  The type of the channel
  */
  public final static AttributeKey<String> IBM_MQ_CHANNEL_TYPE = stringKey("ibm.mq.channel.type");
  

  /**
  The job name
  */
  public final static AttributeKey<String> IBM_MQ_JOB_NAME = stringKey("ibm.mq.job.name");
  

  /**
  The start time of the channel as seconds since Epoch.
  */
  public final static AttributeKey<Long> IBM_MQ_CHANNEL_START_TIME = longKey("ibm.mq.channel.start.time");
  

  /**
  The queue type
  */
  public final static AttributeKey<String> IBM_MQ_QUEUE_TYPE = stringKey("ibm.mq.queue.type");
  

  /**
  The listener name
  */
  public final static AttributeKey<String> IBM_MQ_LISTENER_NAME = stringKey("ibm.mq.listener.name");
  

  /**
  Short name or login/username of the user.
  */
  public final static AttributeKey<String> USER_NAME = stringKey("user.name");
  

  /**
  Logical name of the service.
  */
  public final static AttributeKey<String> SERVICE_NAME = stringKey("service.name");
  

  /**
  The reason code associated with an error
  */
  public final static AttributeKey<String> ERROR_CODE = stringKey("error.code");
  

  private IbmMqAttributes(){}
}