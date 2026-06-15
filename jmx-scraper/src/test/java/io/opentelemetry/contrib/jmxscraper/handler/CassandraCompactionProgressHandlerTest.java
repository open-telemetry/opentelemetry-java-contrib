/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CassandraCompactionProgressHandlerTest {

  private MBeanServerConnection connection;
  private ObjectName objectName;

  @BeforeEach
  void setUp() throws Exception {
    connection = mock(MBeanServerConnection.class);
    objectName = new ObjectName("org.apache.cassandra.db:type=CompactionManager");
  }

  @Test
  void groupsByCompositeKey() throws Exception {
    when(connection.getAttribute(objectName, "Compactions"))
        .thenReturn(
            Arrays.asList(
                compactionEntry("COMPACTION", "ks1", "cf1", "100", "200"),
                compactionEntry("COMPACTION", "ks1", "cf1", "50", "150"),
                compactionEntry("COMPACTION", "ks2", "cf2", "10", "100")));

    Map<Attributes, long[]> groups =
        CassandraCompactionProgressHandler.queryCompactions(connection, objectName);

    assertThat(groups).hasSize(2);
    assertThat(groups.get(attrs("COMPACTION", "ks1", "cf1"))).containsExactly(150L, 350L);
    assertThat(groups.get(attrs("COMPACTION", "ks2", "cf2"))).containsExactly(10L, 100L);
  }

  @Test
  void skipsEntriesMissingDimensionFields() throws Exception {
    when(connection.getAttribute(objectName, "Compactions"))
        .thenReturn(
            Arrays.asList(
                compactionEntry("COMPACTION", "ks1", "cf1", "10", "100"),
                compactionEntry("COMPACTION", null, "cf1", "5", "50"),
                compactionEntry("COMPACTION", "ks1", null, "5", "50")));

    Map<Attributes, long[]> groups =
        CassandraCompactionProgressHandler.queryCompactions(connection, objectName);

    assertThat(groups).hasSize(1);
    assertThat(groups.get(attrs("COMPACTION", "ks1", "cf1"))).containsExactly(10L, 100L);
  }

  @Test
  void skipsEntriesWithNonByteUnits() throws Exception {
    when(connection.getAttribute(objectName, "Compactions"))
        .thenReturn(
            Arrays.asList(
                compactionEntry("COMPACTION", "ks1", "cf1", "10", "100", "bytes"),
                compactionEntry("VALIDATION", "ks1", "cf1", "5", "50", "keys"),
                compactionEntry("ANTICOMPACTION", "ks1", "cf1", "5", "50", "ranges"),
                compactionEntry("COMPACTION", "ks2", "cf2", "5", "50", null)));

    Map<Attributes, long[]> groups =
        CassandraCompactionProgressHandler.queryCompactions(connection, objectName);

    assertThat(groups).hasSize(1);
    assertThat(groups.get(attrs("COMPACTION", "ks1", "cf1"))).containsExactly(10L, 100L);
  }

  @Test
  void parsesBigIntegerStringValues() throws Exception {
    // values larger than Long.MAX_VALUE are truncated but must not throw
    String big = "99999999999999999999";
    when(connection.getAttribute(objectName, "Compactions"))
        .thenReturn(Collections.singletonList(compactionEntry("COMPACTION", "ks", "cf", big, big)));

    Map<Attributes, long[]> groups =
        CassandraCompactionProgressHandler.queryCompactions(connection, objectName);

    assertThat(groups).hasSize(1);
  }

  @Test
  void returnsEmptyMapOnException() throws Exception {
    when(connection.getAttribute(objectName, "Compactions"))
        .thenThrow(new RuntimeException("connection lost"));

    Map<Attributes, long[]> groups =
        CassandraCompactionProgressHandler.queryCompactions(connection, objectName);

    assertThat(groups).isEmpty();
  }

  @Test
  void handlerNameIsStable() {
    assertThat(new CassandraCompactionProgressHandler().getName())
        .isEqualTo(CassandraCompactionProgressHandler.HANDLER_NAME);
  }

  private static Map<String, String> compactionEntry(
      String taskType, String keyspace, String columnfamily, String completed, String total) {
    return compactionEntry(taskType, keyspace, columnfamily, completed, total, "bytes");
  }

  private static Map<String, String> compactionEntry(
      String taskType,
      String keyspace,
      String columnfamily,
      String completed,
      String total,
      String unit) {
    Map<String, String> entry = new HashMap<>();
    entry.put("taskType", taskType);
    entry.put("keyspace", keyspace);
    entry.put("columnfamily", columnfamily);
    entry.put("completed", completed);
    entry.put("total", total);
    entry.put("unit", unit);
    return entry;
  }

  private static Attributes attrs(String taskType, String keyspace, String columnFamily) {
    return Attributes.builder()
        .put("taskType", taskType)
        .put("keyspace", keyspace)
        .put("columnfamily", columnFamily)
        .build();
  }
}
