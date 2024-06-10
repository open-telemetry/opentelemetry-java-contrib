package io.opentelemetry.contrib.inferredspans.semconv;

import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AttributesTest {

  @Test
  public void checkCodeStacktraceUpToDate() {
    assertThat(Attributes.CODE_STACKTRACE).isEqualTo(CodeIncubatingAttributes.CODE_STACKTRACE);
  }
}
