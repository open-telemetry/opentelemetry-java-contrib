/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.statical.instrumenter;

import static io.opentelemetry.contrib.statical.instrumenter.JarTestUtil.assertJar;
import static io.opentelemetry.contrib.statical.instrumenter.JarTestUtil.createJar;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MainTest {

  @TempDir private File tempDir;

  @Test
  public void shouldSaveTransformedJarsTo(@TempDir File target) throws IOException {

    // given
    ClassArchive.Factory factory = mock(ClassArchive.Factory.class);
    ClassArchive mockArchive = mock(ClassArchive.class);
    given(factory.createFor(any(), anyMap())).willReturn(mockArchive);
    Main underTest = new Main(factory);
    underTest.getAdditionalClasses().put("additionalOne", new byte[0]);
    underTest.getAdditionalClasses().put("additionalTwo", new byte[0]);
    String[] jarsList = {
      createJar(tempDir, "one.jar", "first", "second"), createJar(tempDir, "two.jar", "a", "b", "c")
    };
    // when
    underTest.saveTransformedJarsTo(jarsList, target);

    // then
    assertJar(
        target,
        "one.jar",
        new String[] {"additionalOne", "additionalTwo"},
        new byte[][] {null, null});
    assertJar(
        target,
        "one.jar",
        new String[] {"additionalOne", "additionalTwo"},
        new byte[][] {null, null});

    verify(factory, times(2)).createFor(any(), anyMap());
    verify(mockArchive, times(2)).copyAllClassesTo(any());
  }
}
