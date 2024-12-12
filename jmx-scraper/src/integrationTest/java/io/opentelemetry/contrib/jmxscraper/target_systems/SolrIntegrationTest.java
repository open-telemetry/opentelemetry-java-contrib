/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxscraper.target_systems;

import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attribute;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeGroup;
import static io.opentelemetry.contrib.jmxscraper.assertions.DataPointAttributes.attributeWithAnyValue;

import io.opentelemetry.contrib.jmxscraper.JmxScraperContainer;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcher;
import io.opentelemetry.contrib.jmxscraper.assertions.AttributeMatcherGroup;
import java.nio.file.Path;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class SolrIntegrationTest extends TargetSystemIntegrationTest {

  @Override
  protected GenericContainer<?> createTargetContainer(int jmxPort) {
    return new GenericContainer<>("solr:8.8.2")
        .withNetwork(Network.SHARED)
        .withEnv("LOCAL_JMX", "no")
        .withEnv("ENABLE_REMOTE_JMX_OPTS", "true")
        .withEnv("RMI_PORT", Integer.toString(jmxPort))
        .withCommand("solr-precreate", "gettingstarted")
        .withExposedPorts(jmxPort)
        .withStartupTimeout(Duration.ofMinutes(2))
        .waitingFor(Wait.forListeningPort());
  }

  @Override
  protected JmxScraperContainer customizeScraperContainer(
      JmxScraperContainer scraper, GenericContainer<?> target, Path tempDir) {
    return scraper.withTargetSystem("solr");
  }

  @Override
  protected MetricsVerifier createMetricsVerifier() {

    AttributeMatcher coreAttribute = attribute("core", "gettingstarted");
    AttributeMatcherGroup[] requestAttributes = requestAttributes(coreAttribute);
    AttributeMatcherGroup cacheAttributes =
        attributeGroup(coreAttribute, attribute("cache", "searcher"));

    return MetricsVerifier.create()
        .add(
            "solr.document.count",
            metric ->
                metric
                    .hasDescription("The total number of indexed documents.")
                    .hasUnit("{document}")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(coreAttribute))
        .add(
            "solr.index.size",
            metric ->
                metric
                    .hasDescription("The total index size.")
                    .hasUnit("By")
                    .isUpDownCounter()
                    .hasDataPointsWithOneAttribute(coreAttribute))
        .add(
            "solr.request.count",
            metric ->
                metric
                    .hasDescription("The number of queries made.")
                    .hasUnit("{query}")
                    .isCounter()
                    .hasDataPointsWithAttributes(requestAttributes))
        .add(
            "solr.request.time.average",
            metric ->
                metric
                    .hasDescription(
                        "The average time of a query, based on Solr's histogram configuration.")
                    .hasUnit("ms")
                    .isGauge()
                    .hasDataPointsWithAttributes(requestAttributes))
        .add(
            "solr.request.error.count",
            metric ->
                metric
                    .hasDescription("The number of queries resulting in an error.")
                    .hasUnit("{query}")
                    .isCounter()
                    .hasDataPointsWithAttributes(requestAttributes))
        .add(
            "solr.request.timeout.count",
            metric ->
                metric
                    .hasDescription("The number of queries resulting in a timeout.")
                    .hasUnit("{query}")
                    .isCounter()
                    .hasDataPointsWithAttributes(requestAttributes))
        .add(
            "solr.cache.eviction.count",
            metric ->
                metric
                    .hasDescription("The number of evictions from a cache.")
                    .hasUnit("{eviction}")
                    .isCounter()
                    .hasDataPointsWithAttributes(cacheAttributes))
        .add(
            "solr.cache.hit.count",
            metric ->
                metric
                    .hasDescription("The number of hits for a cache.")
                    .hasUnit("{hit}")
                    .isCounter()
                    .hasDataPointsWithAttributes(cacheAttributes))
        .add(
            "solr.cache.insert.count",
            metric ->
                metric
                    .hasDescription("The number of inserts to a cache.")
                    .hasUnit("{insert}")
                    .isCounter()
                    .hasDataPointsWithAttributes(cacheAttributes))
        .add(
            "solr.cache.lookup.count",
            metric ->
                metric
                    .hasDescription("The number of lookups to a cache.")
                    .hasUnit("{lookup}")
                    .isCounter()
                    .hasDataPointsWithAttributes(cacheAttributes))
        .add(
            "solr.cache.size",
            metric ->
                metric
                    .hasDescription("The size of the cache occupied in memory.")
                    .hasUnit("By")
                    .isUpDownCounter()
                    .hasDataPointsWithAttributes(cacheAttributes));
  }

  private static AttributeMatcherGroup[] requestAttributes(AttributeMatcher coreAttribute) {
    // ignore actual 'handler' values due to high cardinality (10+) and an exact matching makes test
    // flaky
    return new AttributeMatcherGroup[] {
      attributeGroup(coreAttribute, attributeWithAnyValue("handler"), attribute("type", "QUERY")),
      attributeGroup(coreAttribute, attributeWithAnyValue("handler"), attribute("type", "UPDATE"))
    };
  }
}
