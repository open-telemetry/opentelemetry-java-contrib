/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

class OpenDataUtils {

  private OpenDataUtils() {}

  /**
   * Convert the Map to TabularData
   *
   * @param options A map of key-value pairs.
   * @return TabularData
   * @throws OpenDataException Can only be raised if there is a bug in this code.
   */
  static TabularData makeOpenData(Map<String, String> options) throws OpenDataException {
    // Copied from newrelic-jfr-core
    String typeName = "java.util.Map<java.lang.String, java.lang.String>";
    String[] itemNames = new String[] {"key", "value"};
    OpenType<?>[] openTypes = new OpenType<?>[] {SimpleType.STRING, SimpleType.STRING};
    CompositeType rowType = new CompositeType(typeName, typeName, itemNames, itemNames, openTypes);
    TabularType tabularType = new TabularType(typeName, typeName, rowType, new String[] {"key"});
    TabularDataSupport table = new TabularDataSupport(tabularType);

    for (Map.Entry<String, String> entry : options.entrySet()) {
      Object[] itemValues = {entry.getKey(), entry.getValue()};
      CompositeData element = new CompositeDataSupport(rowType, itemNames, itemValues);
      table.put(element);
    }
    return table;
  }
}
