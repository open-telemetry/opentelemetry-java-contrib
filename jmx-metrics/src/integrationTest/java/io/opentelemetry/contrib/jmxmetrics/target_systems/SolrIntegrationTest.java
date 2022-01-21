/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics.target_systems;

import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.contrib.jmxmetrics.AbstractIntegrationTest;
import io.opentelemetry.proto.metrics.v1.Metric;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

class SolrIntegrationTest extends AbstractIntegrationTest {

  SolrIntegrationTest() {
    super(/* configFromStdin= */ false, "target-systems/solr.properties");
  }

  @Container
  GenericContainer<?> solr =
      new GenericContainer<>("solr:8.8.2")
          .withNetwork(Network.SHARED)
          .withEnv("LOCAL_JMX", "no")
          .withEnv("ENABLE_REMOTE_JMX_OPTS", "true")
          .withEnv("RMI_PORT", "9990")
          .withCommand("solr-precreate", "gettingstarted")
          .withNetworkAliases("solr")
          .withExposedPorts(9990)
          .withStartupTimeout(Duration.ofMinutes(2))
          .waitingFor(Wait.forListeningPort());

  @Test
  void endToEnd() {
    waitAndAssertMetrics(
        metric ->
            assertSumWithAttributes(
                metric,
                "solr.document.count",
                "The total number of indexed documents.",
                "{documents}",
                attrs -> attrs.containsOnly(entry("core", "gettingstarted"))),
        metric ->
            assertSumWithAttributes(
                metric,
                "solr.index.size",
                "The total index size.",
                "by",
                attrs -> attrs.containsOnly(entry("core", "gettingstarted"))),
        metric ->
            assertSolrRequestSumMetric(
                metric, "solr.request.count", "The number of queries made.", "{queries}"),
        metric ->
            assertSolrRequestGaugeMetric(
                metric,
                "solr.request.time.average",
                "The average time of a query, based on Solr's histogram configuration.",
                "ms"),
        metric ->
            assertSolrRequestSumMetric(
                metric,
                "solr.request.error.count",
                "The number of queries resulting in an error.",
                "{queries}"),
        metric ->
            assertSolrRequestSumMetric(
                metric,
                "solr.request.timeout.count",
                "The number of queries resulting in a timeout.",
                "{queries}"),
        metric ->
            assertSolrCacheSumMetric(
                metric,
                "solr.cache.eviction.count",
                "The number of evictions from a cache.",
                "{evictions}"),
        metric ->
            assertSolrCacheSumMetric(
                metric, "solr.cache.hit.count", "The number of hits for a cache.", "{hits}"),
        metric ->
            assertSolrCacheSumMetric(
                metric,
                "solr.cache.insert.count",
                "The number of inserts to a cache.",
                "{inserts}"),
        metric ->
            assertSolrCacheSumMetric(
                metric,
                "solr.cache.lookup.count",
                "The number of lookups to a cache.",
                "{lookups}"),
        metric ->
            assertSolrCacheSumMetric(
                metric, "solr.cache.size", "The size of the cache occupied in memory.", "by"));
  }

  private void assertSolrRequestSumMetric(
      Metric metric, String name, String description, String unit) {
    assertSumWithAttributes(
        metric,
        name,
        description,
        unit,
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"), entry("handler", "/get"), entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update/csv"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/query"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/graph"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "update"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/debug/dump"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update/json"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/stream"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/export"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update/json/docs"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"), entry("handler", "/sql"), entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/select"),
                entry("type", "QUERY")));
  }

  private void assertSolrRequestGaugeMetric(
      Metric metric, String name, String description, String unit) {
    assertGaugeWithAttributes(
        metric,
        name,
        description,
        unit,
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"), entry("handler", "/get"), entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update/csv"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/query"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/graph"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "update"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/debug/dump"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update/json"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/stream"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/export"),
                entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/update/json/docs"),
                entry("type", "UPDATE")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"), entry("handler", "/sql"), entry("type", "QUERY")),
        attrs ->
            attrs.containsExactly(
                entry("core", "gettingstarted"),
                entry("handler", "/select"),
                entry("type", "QUERY")));
  }

  private void assertSolrCacheSumMetric(
      Metric metric, String name, String description, String unit) {
    assertSumWithAttributes(
        metric,
        name,
        description,
        unit,
        attrs ->
            attrs.containsExactly(entry("core", "gettingstarted"), entry("cache", "searcher")));
  }
}
