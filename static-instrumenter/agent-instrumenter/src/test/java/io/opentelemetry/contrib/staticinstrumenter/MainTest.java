/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import static io.opentelemetry.contrib.staticinstrumenter.JarTestUtil.getResourcePath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MainTest {

  @Test
  void shouldInjectAdditionalClasses(@TempDir File destination) throws IOException {

    // given
    ClassArchive.Factory factory = mock(ClassArchive.Factory.class);
    ClassArchive mockArchive = mock(ClassArchive.class);
    given(factory.createFor(any(), anyMap())).willReturn(mockArchive);
    Main underTest = new Main(factory);
    underTest.getAdditionalClasses().put("additionalOne.class", new byte[0]);
    underTest.getAdditionalClasses().put("additionalTwo.class", new byte[0]);

    // when
    underTest.saveTransformedJarsTo(new String[] {getResourcePath("test.jar")}, destination);

    // then
    JarTestUtil.assertJar(
        destination, "test.jar", new String[] {"additionalOne.class", "additionalTwo.class"}, null);
  }
}
