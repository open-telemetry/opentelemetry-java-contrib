/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.messaging.wrappers.mns;

import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.MessagePropertyValue;
import com.aliyun.mns.model.MessageSystemPropertyName;
import com.aliyun.mns.model.MessageSystemPropertyValue;
import com.aliyun.mns.model.SystemPropertyType;
import io.opentelemetry.context.propagation.TextMapSetter;
import javax.annotation.Nullable;

public enum MnsTextMapSetter implements TextMapSetter<Message> {
  INSTANCE;

  /**
   * MNS message trails currently only support the W3C protocol; other protocol headers should be
   * injected into userProperties.
   */
  @Override
  public void set(@Nullable Message message, String key, String value) {
    if (message == null) {
      return;
    }
    MessageSystemPropertyName systemPropertyName = MnsHelper.convert2SystemPropertyName(key);
    if (systemPropertyName != null) {
      message.putSystemProperty(
          systemPropertyName, new MessageSystemPropertyValue(SystemPropertyType.STRING, value));
    } else {
      message.getUserProperties().put(key, new MessagePropertyValue(value));
    }
  }
}
