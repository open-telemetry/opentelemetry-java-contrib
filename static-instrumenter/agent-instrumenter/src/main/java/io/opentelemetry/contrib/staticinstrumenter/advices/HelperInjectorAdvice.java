/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.advices;

import io.opentelemetry.contrib.staticinstrumenter.internals.Main;
import java.util.Map;
import net.bytebuddy.asm.Advice;

@SuppressWarnings("unused")
public final class HelperInjectorAdvice {

  @Advice.OnMethodEnter(suppress = Throwable.class)
  static void enter(@Advice.Argument(value = 0) Map<String, byte[]> classnameToBytes) {

    for (String className : classnameToBytes.keySet()) {
      String modifiedClassName = className.replace(".", "/") + ".class";

      Main.getInstance()
          .getAdditionalClasses()
          .put(modifiedClassName, classnameToBytes.get(className));
    }
  }

  private HelperInjectorAdvice() {}
}
