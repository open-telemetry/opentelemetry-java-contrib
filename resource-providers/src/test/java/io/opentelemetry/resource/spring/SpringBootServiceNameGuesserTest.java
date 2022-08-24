/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resource.spring;

import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpringBootServiceNameGuesserTest {

  public static final String PROPS = "application.properties";
  public static final String APPLICATION_YML = "application.yml";
  @Mock ConfigProperties config;
  @Mock SpringBootServiceNameGuesser.SystemHelper system;

  @Test
  void findByEnvVar() {
    var expected = "fur-city";
    when(system.getenv("SPRING_APPLICATION_NAME")).thenReturn(expected);

    var guesser = new SpringBootServiceNameGuesser(system);

    var result = guesser.createResource(config);
    expectServiceName(result, expected);
  }

  @Test
  void classpathApplicationProperties() {
    when(system.openClasspathResource(PROPS)).thenCallRealMethod();
    var guesser = new SpringBootServiceNameGuesser(system);
    var result = guesser.createResource(config);
    expectServiceName(result, "dog-store");
  }

  @Test
  void propertiesFileInCurrentDir() throws Exception {
    var propsPath = Paths.get(PROPS);
    try {
      Files.writeString(propsPath, "spring.application.name=fish-tank\n");
      when(system.openFile(PROPS)).thenCallRealMethod();
      var guesser = new SpringBootServiceNameGuesser(system);
      var result = guesser.createResource(config);
      expectServiceName(result, "fish-tank");
    } finally {
      Files.delete(propsPath);
    }
  }

  @Test
  void classpathApplicationYaml() {
    when(system.openClasspathResource(APPLICATION_YML)).thenCallRealMethod();
    var guesser = new SpringBootServiceNameGuesser(system);
    var result = guesser.createResource(config);
    expectServiceName(result, "cat-store");
  }

  @Test
  void yamlFileInCurrentDir() throws Exception {
    var yamlPath = Paths.get(APPLICATION_YML);
    try {
      var url = getClass().getClassLoader().getResource(APPLICATION_YML);
      var content = Files.readString(Paths.get(url.toURI()));
      Files.writeString(yamlPath, content);
      when(system.openFile(PROPS)).thenThrow(new FileNotFoundException());
      when(system.openFile(APPLICATION_YML)).thenCallRealMethod();
      var guesser = new SpringBootServiceNameGuesser(system);
      var result = guesser.createResource(config);
      expectServiceName(result, "cat-store");
    } finally {
      Files.delete(yamlPath);
    }
  }

  @Test
  void getFromCommandlineArgsWithProcessHandle() throws Exception {
    when(system.attemptGetCommandLineViaReflection())
        .thenReturn(
            "/bin/java sweet-spring.jar --spring.application.name=tiger-town --quiet=never");
    var guesser = new SpringBootServiceNameGuesser(system);
    var result = guesser.createResource(config);
    expectServiceName(result, "tiger-town");
  }

  @Test
  void getFromCommandlineArgsWithSystemProperty() throws Exception {
    when(system.getProperty("spring.application.name")).thenReturn(null);
    when(system.getProperty("sun.java.command"))
        .thenReturn("/bin/java sweet-spring.jar --spring.application.name=bullpen --quiet=never");
    var guesser = new SpringBootServiceNameGuesser(system);
    var result = guesser.createResource(config);
    expectServiceName(result, "bullpen");
  }

  private static void expectServiceName(Resource result, String expected) {
    assertThat(result.getAttribute(SERVICE_NAME)).isEqualTo(expected);
  }
}
