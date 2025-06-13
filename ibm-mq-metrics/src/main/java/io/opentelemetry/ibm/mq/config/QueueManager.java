/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.ibm.mq.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueManager {

  private String host;
  private int port = -1;
  private String name;
  private String channelName;
  private String transportType;
  private String username;
  private String password;
  private String sslKeyRepository;
  private int ccsid = Integer.MIN_VALUE;
  private int encoding = Integer.MIN_VALUE;
  private String cipherSuite;
  private String cipherSpec;
  private String replyQueuePrefix;
  private String modelQueueName;
  private String configurationQueueName = "SYSTEM.ADMIN.CONFIG.EVENT";
  private String performanceEventsQueueName = "SYSTEM.ADMIN.PERFM.EVENT";
  private String queueManagerEventsQueueName = "SYSTEM.ADMIN.QMGR.EVENT";
  private long consumeConfigurationEventInterval;
  private boolean refreshQueueManagerConfigurationEnabled;
  // Config default is 100.
  // https://www.ibm.com/docs/en/ibm-mq/9.3.x?topic=qmini-channels-stanza-file
  private int maxActiveChannels = 100;

  private ResourceFilters queueFilters;
  private ResourceFilters channelFilters;
  private ResourceFilters listenerFilters;
  private ResourceFilters topicFilters;

  List<String> writeStatsDirectory;

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

  public String getChannelName() {
    return channelName;
  }

  public void setChannelName(String channelName) {
    this.channelName = channelName;
  }

  public String getTransportType() {
    return transportType;
  }

  public void setTransportType(String transportType) {
    this.transportType = transportType;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public ResourceFilters getQueueFilters() {
    if (queueFilters == null) {
      return new ResourceFilters();
    }
    return queueFilters;
  }

  public void setQueueFilters(ResourceFilters queueFilters) {
    this.queueFilters = queueFilters;
  }

  public String getSslKeyRepository() {
    return sslKeyRepository;
  }

  public void setSslKeyRepository(String sslKeyRepository) {
    this.sslKeyRepository = sslKeyRepository;
  }

  public String getCipherSuite() {
    return cipherSuite;
  }

  public void setCipherSuite(String cipherSuite) {
    this.cipherSuite = cipherSuite;
  }

  public String getCipherSpec() {
    return cipherSpec;
  }

  public void setCipherSpec(String cipherSpec) {
    this.cipherSpec = cipherSpec;
  }

  public ResourceFilters getChannelFilters() {
    if (channelFilters == null) {
      return new ResourceFilters();
    }
    return channelFilters;
  }

  public void setChannelFilters(ResourceFilters channelFilters) {
    this.channelFilters = channelFilters;
  }

  public String getReplyQueuePrefix() {
    return replyQueuePrefix;
  }

  public void setReplyQueuePrefix(String replyQueuePrefix) {
    this.replyQueuePrefix = replyQueuePrefix;
  }

  public String getModelQueueName() {
    return modelQueueName;
  }

  public void setModelQueueName(String modelQueueName) {
    this.modelQueueName = modelQueueName;
  }

  public ResourceFilters getListenerFilters() {
    if (listenerFilters == null) {
      return new ResourceFilters();
    }
    return listenerFilters;
  }

  public void setListenerFilters(ResourceFilters listenerFilters) {
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

  public void setTopicFilters(ResourceFilters topicFilters) {
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
