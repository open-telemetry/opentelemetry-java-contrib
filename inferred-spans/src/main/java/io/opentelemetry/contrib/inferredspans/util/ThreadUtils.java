/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
