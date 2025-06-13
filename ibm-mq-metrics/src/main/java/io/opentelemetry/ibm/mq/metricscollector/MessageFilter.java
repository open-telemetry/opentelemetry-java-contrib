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
package io.opentelemetry.ibm.mq.metricscollector;

import com.ibm.mq.headers.pcf.PCFException;
import com.ibm.mq.headers.pcf.PCFMessage;
import io.opentelemetry.ibm.mq.config.ExcludeFilters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helps to consolidate repeated exclude/filtering logic. */
class MessageFilter {

  private static final Logger logger = LoggerFactory.getLogger(MessageFilter.class);

  private final String kind;
  private final Collection<ExcludeFilters> filters;
  private final ResourceExtractor extractor;

  private MessageFilter(
      String kind, Collection<ExcludeFilters> filters, ResourceExtractor extractor) {
    this.kind = kind;
    this.filters = filters;
    this.extractor = extractor;
  }

  static MessageFilterBuilder ofKind(String kind) {
    return new MessageFilterBuilder(kind);
  }

  public List<PCFMessage> filter(List<PCFMessage> messages) throws PCFException {
    List<PCFMessage> result = new ArrayList<>();
    for (PCFMessage message : messages) {
      String resourceName = extractor.apply(message);
      if (ExcludeFilters.isExcluded(resourceName, filters)) {
        logger.debug("{} name = {} is excluded.", kind, resourceName);
      } else {
        result.add(message);
      }
    }
    return result;
  }

  static class MessageFilterBuilder {

    private Collection<ExcludeFilters> filters;
    private String kind;

    public MessageFilterBuilder(String kind) {
      this.kind = kind;
    }

    public MessageFilterBuilder excluding(Collection<ExcludeFilters> filters) {
      this.filters = filters;
      return this;
    }

    public MessageFilter withResourceExtractor(ResourceExtractor extractor) {
      return new MessageFilter(kind, filters, extractor);
    }
  }

  interface ResourceExtractor {
    // Ugh, exceptions everywhere, huh?
    String apply(PCFMessage message) throws PCFException;
  }
}
