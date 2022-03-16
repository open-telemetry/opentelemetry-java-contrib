/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javax.annotation.Nullable;

public class PreTransformer implements ClassFileTransformer {

  @Override
  @Nullable
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer) {

    Main.getInstance().setCurrentClass(new TransformedClass(className, classfileBuffer));
    return null;
  }
}
