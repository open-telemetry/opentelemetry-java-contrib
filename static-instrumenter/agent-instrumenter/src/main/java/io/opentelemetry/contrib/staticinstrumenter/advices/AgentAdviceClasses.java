/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.advices;

import io.opentelemetry.contrib.staticinstrumenter.internals.Main;
import java.lang.instrument.Instrumentation;
import net.bytebuddy.asm.Advice;

public final class AgentAdviceClasses {
  @SuppressWarnings("unused")
  public static final class InstallBootstrapJarAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    static void enterInstallBoostrapJar(@Advice.Argument(value = 0) Instrumentation inst) {
      inst.addTransformer(Main.getPreTransformer());
    }

    private InstallBootstrapJarAdvice() {}
  }

  @SuppressWarnings("unused")
  public static final class AgentMainAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    static void exitStartAgent(@Advice.Argument(value = 0) Instrumentation inst) {
      inst.addTransformer(Main.getPostTransformer(), true);
    }

    private AgentMainAdvice() {}
  }

  private AgentAdviceClasses() {}
}
