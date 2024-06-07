/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ThreadUtils {

  private static final MethodHandle VIRTUAL_CHECKER = generateVirtualChecker();

  public static boolean isVirtual(Thread thread) {
    try {
      return (boolean) VIRTUAL_CHECKER.invokeExact(thread);
    } catch (Throwable e) {
      throw new IllegalStateException("isVirtual is not expected to throw exceptions", e);
    }
  }

  private static MethodHandle generateVirtualChecker() {
    Method isVirtual = null;
    try {
      isVirtual = Thread.class.getMethod("isVirtual");
      isVirtual.invoke(
          Thread.currentThread()); // invoke to ensure it does not throw exceptions for preview
      // versions
      return MethodHandles.lookup().unreflect(isVirtual);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // virtual threads are not supported, therefore no thread is virtual
      return MethodHandles.dropArguments(
          MethodHandles.constant(boolean.class, false), 0, Thread.class);
    }
  }
}
