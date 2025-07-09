package io.opentelemetry.contrib.cloudfoundry.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ComponentProviderTest {

  @Test
  @SuppressWarnings("rawtypes")
  void providerIsLoaded() {
    List<ComponentProvider> providers = SpiHelper.create(
            ComponentProviderTest.class.getClassLoader())
        .load(ComponentProvider.class);
    assertThat(providers).extracting(ComponentProvider::getName)
        .containsExactly("cloud_foundry");
  }
}
