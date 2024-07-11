/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.internal;

import java.util.Objects;
import javax.annotation.Nullable;

public final class StackFrame {
  @Nullable private final String className;
  private final String methodName;

  public static StackFrame of(@Nullable String className, String methodName) {
    return new StackFrame(className, methodName);
  }

  public StackFrame(@Nullable String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
  }

  @Nullable
  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public int getSimpleClassNameOffset() {
    if (className != null) {
      return className.lastIndexOf('.') + 1;
    }
    return 0;
  }

  public void appendFileName(StringBuilder replaceBuilder) {
    String unknownCodeSource = "<Unknown>";
    if (className != null) {
      int fileNameEnd = className.indexOf('$');
      if (fileNameEnd < 0) {
        fileNameEnd = className.length();
      }
      int classNameStart = className.lastIndexOf('.');
      if (classNameStart < fileNameEnd && fileNameEnd <= className.length()) {
        replaceBuilder.append(className, classNameStart + 1, fileNameEnd);
        replaceBuilder.append(".java");
      } else {
        replaceBuilder.append(unknownCodeSource);
      }
    } else {
      replaceBuilder.append(unknownCodeSource);
    }
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StackFrame that = (StackFrame) o;

    if (!Objects.equals(className, that.className)) {
      return false;
    }
    return methodName.equals(that.methodName);
  }

  @Override
  public int hashCode() {
    int result = className != null ? className.hashCode() : 0;
    result = 31 * result + methodName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    if (className == null) {
      return methodName;
    }
    return className + '.' + methodName;
  }
}
