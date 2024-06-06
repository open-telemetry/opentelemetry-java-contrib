/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.opentelemetry.contrib.inferredspans.util;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class TemporaryProperties implements AutoCloseable {

  private final Map<String, String> originalValues = new HashMap<>();

  public TemporaryProperties put(String key, @Nullable String value) {
    if (!originalValues.containsKey(key)) {
      originalValues.put(key, System.getProperty(key));
    }
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
    return this;
  }

  @Override
  public void close() {
    for (String key : originalValues.keySet()) {
      String value = originalValues.get(key);
      if (value == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, value);
      }
    }
  }
}
