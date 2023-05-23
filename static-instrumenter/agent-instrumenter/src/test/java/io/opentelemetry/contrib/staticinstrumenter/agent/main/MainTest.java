/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.agent.main;

import static io.opentelemetry.contrib.staticinstrumenter.agent.main.JarTestUtil.getResourcePath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MainTest {

  @Test
  void shouldInjectAdditionalClasses(@TempDir File destination) throws IOException {

    // given
    ClassArchive.Factory factory = mock(ClassArchive.Factory.class);
    ClassArchive mockArchive = mock(ClassArchive.class);
    when(factory.createFor(any(), anyMap())).thenReturn(mockArchive);
    Main underTest = new Main(factory);
    AdditionalClasses.put("additionalOne.class", new byte[0]);
    AdditionalClasses.put("additionalTwo.class", new byte[0]);

    // when
    underTest.saveTransformedJarsTo(List.of(getResourcePath("test.jar")), destination);

    // then
    JarTestUtil.assertJar(
        destination, "test.jar", new String[] {"additionalOne.class", "additionalTwo.class"}, null);
  }
}
