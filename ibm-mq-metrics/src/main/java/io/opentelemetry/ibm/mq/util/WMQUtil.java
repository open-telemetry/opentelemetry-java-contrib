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
package io.opentelemetry.ibm.mq.util;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.pcf.PCFMessageAgent;
import io.opentelemetry.ibm.mq.WMQContext;
import io.opentelemetry.ibm.mq.config.QueueManager;
import java.util.Hashtable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WMQUtil {

  private static final Logger logger = LoggerFactory.getLogger(WMQUtil.class);

  private WMQUtil() {}

  public static PCFMessageAgent initPCFMessageAgent(
      QueueManager queueManager, MQQueueManager ibmQueueManager) {
    try {
      PCFMessageAgent agent;
      if (isNotNullOrEmpty(queueManager.getModelQueueName())
          && isNotNullOrEmpty(queueManager.getReplyQueuePrefix())) {
        logger.debug("Initializing the PCF agent for model queue and reply queue prefix.");
        agent = new PCFMessageAgent();
        agent.setModelQueueName(queueManager.getModelQueueName());
        agent.setReplyQueuePrefix(queueManager.getReplyQueuePrefix());
        logger.debug("Connecting to queueManager to set the modelQueueName and replyQueuePrefix.");
        agent.connect(ibmQueueManager);
      } else {
        agent = new PCFMessageAgent(ibmQueueManager);
      }
      if (queueManager.getCcsid() != Integer.MIN_VALUE) {
        agent.setCharacterSet(queueManager.getCcsid());
      }

      if (queueManager.getEncoding() != Integer.MIN_VALUE) {
        agent.setEncoding(queueManager.getEncoding());
      }
      logger.debug(
          "Initialized PCFMessageAgent for queueManager {} in thread {}",
          agent.getQManagerName(),
          Thread.currentThread().getName());
      return agent;
    } catch (MQDataException mqe) {
      logger.error(mqe.getMessage(), mqe);
      throw new RuntimeException(mqe);
    }
  }

  @SuppressWarnings("rawtypes")
  public static MQQueueManager connectToQueueManager(QueueManager queueManager) {
    WMQContext auth = new WMQContext(queueManager);
    Hashtable env = auth.getMQEnvironment();
    try {
      MQQueueManager ibmQueueManager = new MQQueueManager(queueManager.getName(), env);
      logger.debug(
          "MQQueueManager connection initiated for queueManager {} in thread {}",
          queueManager.getName(),
          Thread.currentThread().getName());
      return ibmQueueManager;
    } catch (MQException mqe) {
      logger.error(mqe.getMessage(), mqe);
      throw new RuntimeException(mqe.getMessage());
    }
  }

  private static boolean isNotNullOrEmpty(String str) {
    return str != null && !str.isEmpty();
  }
}
