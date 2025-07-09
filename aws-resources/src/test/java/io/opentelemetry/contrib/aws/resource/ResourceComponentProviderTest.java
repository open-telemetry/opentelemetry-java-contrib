package io.opentelemetry.contrib.aws.resource;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.internal.SpiHelper;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import org.junit.jupiter.api.Test;
import java.util.List;

public class ResourceComponentProviderTest {

  @Test
  @SuppressWarnings("rawtypes")
  void providerIsLoaded() {
    List<ComponentProvider> providers = SpiHelper.create(
            ResourceComponentProviderTest.class.getClassLoader())
        .load(ComponentProvider.class);
    assertThat(providers).extracting(ComponentProvider::getName)
        .containsExactly("aws");
  }
}
