/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.ibm.mq.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.annotation.Nullable;

/** This is a jackson databind class used purely for config. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class QueueManager {

  @Nullable private String host;
  private int port = -1;
  private String name = "UNKNOWN";
  @Nullable private String channelName;
  @Nullable private String transportType;
  @Nullable private String username;
  @Nullable private String password;
  @Nullable private String sslKeyRepository;
  private int ccsid = Integer.MIN_VALUE;
  private int encoding = Integer.MIN_VALUE;
  @Nullable private String cipherSuite;
  @Nullable private String cipherSpec;
  @Nullable private String replyQueuePrefix;
  @Nullable private String modelQueueName;
  private String configurationQueueName = "SYSTEM.ADMIN.CONFIG.EVENT";
  private String performanceEventsQueueName = "SYSTEM.ADMIN.PERFM.EVENT";
  private String queueManagerEventsQueueName = "SYSTEM.ADMIN.QMGR.EVENT";
  private long consumeConfigurationEventInterval;
  private boolean refreshQueueManagerConfigurationEnabled;
  // Config default is 100.
  // https://www.ibm.com/docs/en/ibm-mq/9.3.x?topic=qmini-channels-stanza-file
  private int maxActiveChannels = 100;

  @Nullable private ResourceFilters queueFilters;
  @Nullable private ResourceFilters channelFilters;
  @Nullable private ResourceFilters listenerFilters;
  @Nullable private ResourceFilters topicFilters;

  @Nullable
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Nullable
  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(@Nullable String channelName) {
    this.channelName = channelName;
  }

  @Nullable
  public String getTransportType() {
    return transportType;
  }

  public void setTransportType(@Nullable String transportType) {
    this.transportType = transportType;
  }

  @Nullable
  public String getUsername() {
    return username;
  }

  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  @Nullable
  public String getPassword() {
    return password;
  }

  public void setPassword(@Nullable String password) {
    this.password = password;
  }

  public ResourceFilters getQueueFilters() {
    if (queueFilters == null) {
      return new ResourceFilters();
    }
    return queueFilters;
  }

  public void setQueueFilters(@Nullable ResourceFilters queueFilters) {
    this.queueFilters = queueFilters;
  }

  @Nullable
  public String getSslKeyRepository() {
    return sslKeyRepository;
  }

  public void setSslKeyRepository(@Nullable String sslKeyRepository) {
    this.sslKeyRepository = sslKeyRepository;
  }

  @Nullable
  public String getCipherSuite() {
    return cipherSuite;
  }

  public void setCipherSuite(String cipherSuite) {
    this.cipherSuite = cipherSuite;
  }

  @Nullable
  public String getCipherSpec() {
    return cipherSpec;
  }

  public void setCipherSpec(@Nullable String cipherSpec) {
    this.cipherSpec = cipherSpec;
  }

  public ResourceFilters getChannelFilters() {
    if (channelFilters == null) {
      return new ResourceFilters();
    }
    return channelFilters;
  }

  public void setChannelFilters(@Nullable ResourceFilters channelFilters) {
    this.channelFilters = channelFilters;
  }

  @Nullable
  public String getReplyQueuePrefix() {
    return replyQueuePrefix;
  }

  public void setReplyQueuePrefix(@Nullable String replyQueuePrefix) {
    this.replyQueuePrefix = replyQueuePrefix;
  }

  @Nullable
  public String getModelQueueName() {
    return modelQueueName;
  }

  public void setModelQueueName(@Nullable String modelQueueName) {
    this.modelQueueName = modelQueueName;
  }

  public ResourceFilters getListenerFilters() {
    if (listenerFilters == null) {
      return new ResourceFilters();
    }
    return listenerFilters;
  }

  public void setListenerFilters(@Nullable ResourceFilters listenerFilters) {
    this.listenerFilters = listenerFilters;
  }

  public int getCcsid() {
    return ccsid;
  }

  public void setCcsid(int ccsid) {
    this.ccsid = ccsid;
  }

  public int getEncoding() {
    return encoding;
  }

  public void setEncoding(int encoding) {
    this.encoding = encoding;
  }

  public ResourceFilters getTopicFilters() {
    if (topicFilters == null) {
      return new ResourceFilters();
    }
    return topicFilters;
  }

  public void setTopicFilters(@Nullable ResourceFilters topicFilters) {
    this.topicFilters = topicFilters;
  }

  public String getConfigurationQueueName() {
    return this.configurationQueueName;
  }

  public void setConfigurationQueueName(String configurationQueueName) {
    this.configurationQueueName = configurationQueueName;
  }

  public long getConsumeConfigurationEventInterval() {
    return this.consumeConfigurationEventInterval;
  }

  public void setConsumeConfigurationEventInterval(long consumeConfigurationEventInterval) {
    this.consumeConfigurationEventInterval = consumeConfigurationEventInterval;
  }

  public boolean isRefreshQueueManagerConfigurationEnabled() {
    return refreshQueueManagerConfigurationEnabled;
  }

  public void setRefreshQueueManagerConfigurationEnabled(
      boolean refreshQueueManagerConfigurationEnabled) {
    this.refreshQueueManagerConfigurationEnabled = refreshQueueManagerConfigurationEnabled;
  }

  public String getPerformanceEventsQueueName() {
    return performanceEventsQueueName;
  }

  public void setPerformanceEventsQueueName(String performanceEventsQueueName) {
    this.performanceEventsQueueName = performanceEventsQueueName;
  }

  public String getQueueManagerEventsQueueName() {
    return this.queueManagerEventsQueueName;
  }

  public void setQueueManagerEventsQueueName(String queueManagerEventsQueueName) {
    this.queueManagerEventsQueueName = queueManagerEventsQueueName;
  }

  public int getMaxActiveChannels() {
    return maxActiveChannels;
  }

  public void setMaxActiveChannels(int maxActiveChannels) {
    this.maxActiveChannels = maxActiveChannels;
  }
}
