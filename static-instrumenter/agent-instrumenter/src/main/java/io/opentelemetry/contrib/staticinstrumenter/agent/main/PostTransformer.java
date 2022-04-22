/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.agent.main;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import javax.annotation.Nullable;

public class PostTransformer implements ClassFileTransformer {
  @Override
  @Nullable
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {

    TransformedClass pre = CurrentClass.getAndRemove();

    if (pre != null
        && pre.getName().equals(className)
        && !Arrays.equals(pre.getClasscode(), classfileBuffer)) {
      Main.getInstance().getInstrumentedClasses().put(className, classfileBuffer);
    }
    return null;
  }
}
