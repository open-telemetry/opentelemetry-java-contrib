/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;
import org.junit.jupiter.api.Test;

class OpenDataUtilsTest {

  @Test
  void makeOpenData() throws Exception {
    Map<String, String> configuration = new HashMap<>();
    configuration.put("jdk.ObjectAllocationInNewTLAB#enabled", "true");
    configuration.put("jdk.ObjectAllocationOutsideTLAB#enabled", "true");

    TabularData expected = OpenDataUtils.makeOpenData(configuration);

    MBeanServerConnection mBeanServerConnection = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName("jdk.management.jfr:type=FlightRecorder");
    ObjectInstance objectInstance = mBeanServerConnection.getObjectInstance(objectName);

    Object[] args = new Object[] {};
    String[] argTypes = new String[] {};
    long id =
        (long)
            mBeanServerConnection.invoke(
                objectInstance.getObjectName(), "newRecording", args, argTypes);

    args = new Object[] {id, expected};
    argTypes = new String[] {long.class.getName(), TabularData.class.getName()};
    mBeanServerConnection.invoke(
        objectInstance.getObjectName(), "setRecordingSettings", args, argTypes);

    args = new Object[] {id};
    argTypes = new String[] {long.class.getName()};
    Map<?, ?> actual =
        (Map<?, ?>)
            mBeanServerConnection.invoke(
                objectInstance.getObjectName(), "getRecordingSettings", args, argTypes);

    assertEquals(expected, actual);
  }
}
