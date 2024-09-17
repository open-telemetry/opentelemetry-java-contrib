/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jmxmetrics;

import static java.nio.charset.StandardCharsets.UTF_8;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.groovy.control.CompilationFailedException;

public class GroovyRunner {
  private static final Logger logger = Logger.getLogger(GroovyRunner.class.getName());

  private final List<Script> scripts;
  private final GroovyMetricEnvironment groovyMetricEnvironment;

  GroovyRunner(
      final JmxConfig config,
      final JmxClient jmxClient,
      final GroovyMetricEnvironment groovyMetricEnvironment) {
    this.groovyMetricEnvironment = groovyMetricEnvironment;

    List<String> scriptSources = new ArrayList<>();
    try {
      if (config.targetSystems.size() != 0) {
        for (final String target : config.targetSystems) {
          String systemResourcePath = "target-systems/" + target + ".groovy";
          scriptSources.add(getTargetSystemResourceAsString(systemResourcePath));
        }
      }
      if (config.groovyScript != null && !config.groovyScript.isEmpty()) {
        scriptSources.add(getFileAsString(config.groovyScript));
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to read groovy script", e);
      throw new ConfigurationException("Failed to read groovy script", e);
    }

    this.scripts = new ArrayList<>();
    try {
      for (final String source : scriptSources) {
        this.scripts.add(new GroovyShell().parse(source));
      }
    } catch (CompilationFailedException e) {
      logger.log(Level.SEVERE, "Failed to compile groovy script", e);
      throw new ConfigurationException("Failed to compile groovy script", e);
    }

    Binding binding = new Binding();
    binding.setVariable("log", logger);

    OtelHelper otelHelper =
        new OtelHelper(jmxClient, this.groovyMetricEnvironment, config.aggregateAcrossMBeans);
    binding.setVariable("otel", otelHelper);

    for (final Script script : scripts) {
      script.setBinding(binding);
    }
  }

  private static String getFileAsString(final String fileName) throws IOException {
    try (InputStream is = new FileInputStream(fileName)) {
      return getFileFromInputStream(is);
    }
  }

  private String getTargetSystemResourceAsString(final String targetSystem) throws IOException {
    URL res = getClass().getClassLoader().getResource(targetSystem);
    if (res == null) {
      throw new ConfigurationException("Failed to load " + targetSystem);
    }

    if (res.toString().contains("!")) {
      return getTargetSystemResourceFromJarAsString(res);
    }
    return getFileAsString(res.getPath());
  }

  private static String getTargetSystemResourceFromJarAsString(URL res) throws IOException {
    final String[] array = res.toString().split("!");
    if (array.length != 2) {
      throw new ConfigurationException(
          "Invalid path for target system resource from jar: " + res.toString());
    }

    final Map<String, String> env = Collections.emptyMap();
    try {
      try (final FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), env)) {
        Path path = fs.getPath(array[1]);
        try (InputStream is = Files.newInputStream(path)) {
          return getFileFromInputStream(is);
        }
      }
    } catch (IOException e) {
      throw new ConfigurationException("Failed to load " + res.toString(), e);
    }
  }

  private static String getFileFromInputStream(final InputStream is) throws IOException {
    if (is == null) {
      return null;
    }
    try (InputStreamReader isr = new InputStreamReader(is, UTF_8);
        BufferedReader reader = new BufferedReader(isr)) {
      StringBuilder file = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        file.append(System.lineSeparator());
        file.append(line);
      }
      return file.toString();
    }
  }

  public void run() {
    for (final Script script : scripts) {
      script.run();
    }
  }

  // Visible for testing
  List<Script> getScripts() {
    return scripts;
  }
}
