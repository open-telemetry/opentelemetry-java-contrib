package io.opentelemetry.maven.resources;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MavenResourceProviderTest {

  @Test
  void testGetMavenVersion() {
    String mavenVersion = MavenResourceProvider.getMavenRuntimeVersion();
    assertThat(mavenVersion).isEqualTo("3.5.0");
  }
}
