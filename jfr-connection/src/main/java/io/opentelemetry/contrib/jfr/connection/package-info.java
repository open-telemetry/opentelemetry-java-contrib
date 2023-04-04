/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * This package provides API for controlling Java Flight Recordings (JFR) through Java Management
 * Extensions (JMX). The main entry point is {@link
 * io.opentelemetry.contrib.jfr.connection.FlightRecorderConnection}.
 *
 * <p>JDK 9 introduced the {@code jdk.jfr} API which is not available in JDK 8. The {@code
 * jdk.management.jfr.FlightRecorderMXBean} is available in OpenJDK 8u262 and higher. By relying on
 * JMX and the {@code jdk.management.jfr.FlightRecorderMXBean}, the {@code
 * io.opentelemetry.contrib.jfr.connection} package provides access to JFR on local or remote JVMs.
 */
package io.opentelemetry.contrib.jfr.connection;
