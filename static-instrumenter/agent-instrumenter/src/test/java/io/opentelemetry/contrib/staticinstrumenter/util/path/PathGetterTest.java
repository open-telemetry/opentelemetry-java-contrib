/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.util.path;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.jar.JarEntry;
import org.junit.jupiter.api.Test;

public class PathGetterTest {

  @Test
  public void testInst() {

    SimplePathGetter pathGetter = new SimplePathGetter();

    String newName = pathGetter.getPath(new JarEntry("inst/aaa/bbb/ccc.class"));

    assertThat(newName).isEqualTo("aaa/bbb/ccc.class");
  }

  @Test
  public void instInTheMiddle() {

    SimplePathGetter pathGetter = new SimplePathGetter();

    String newName = pathGetter.getPath(new JarEntry("aaa/inst/ccc.class"));
    String newName2 = pathGetter.getPath(new JarEntry("inst/aaa/inst/ccc.class"));

    assertThat(newName).isEqualTo("aaa/inst/ccc.class");
    assertThat(newName2).isEqualTo("aaa/inst/ccc.class");
  }

  @Test
  public void classData() {

    SimplePathGetter pathGetter = new SimplePathGetter();

    String newName = pathGetter.getPath(new JarEntry("inst/aaa/inst/ccc.classdata"));

    assertThat(newName).isEqualTo("aaa/inst/ccc.class");
  }

  @Test
  public void simpleEntry() {

    SimplePathGetter pathGetter = new SimplePathGetter();

    String newName = pathGetter.getPath(new JarEntry("classdata/aaa/inst/ccc.class"));

    assertThat(newName).isEqualTo("classdata/aaa/inst/ccc.class");
  }

  @Test
  public void testIdentityPathGetter() {

    IdentityPathGetter pathGetter = new IdentityPathGetter();

    String newName = pathGetter.getPath(new JarEntry("inst/aaa/inst.classdata/ccc.classdata"));

    assertThat(newName).isEqualTo("inst/aaa/inst.classdata/ccc.classdata");
  }
}
