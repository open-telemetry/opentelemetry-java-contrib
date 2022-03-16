/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

class ArchiveEntry {

  private static final ArchiveEntry NOT_CLASS =
      new ArchiveEntry("", /* isClass= */ false, /* shouldBeSkipped= */ true);

  private final String className;
  private final String classPath;
  private final boolean isClass;
  private final boolean shouldBeSkipped;

  private ArchiveEntry(String className, boolean isClass, boolean shouldBeSkipped) {
    this.className = className;
    this.classPath = className.replace(".", "/");
    this.isClass = isClass;
    this.shouldBeSkipped = shouldBeSkipped;
  }

  static ArchiveEntry ofFilePath(String path) {
    if (!isClass(path)) {
      return NOT_CLASS;
    }
    return new ArchiveEntry(
        className(path), /* isClass= */ true, /* shouldBeSkipped= */ shouldBeSkipped(path));
  }

  String getClassName() {
    return className;
  }

  private static String className(String path) {
    return path.substring(0, path.indexOf(".class")).replace("/", ".");
  }

  String getClassPath() {
    return classPath;
  }

  boolean isClass() {
    return isClass;
  }

  private static boolean isClass(String path) {
    return path.endsWith(".class");
  }

  boolean shouldBeSkipped() {
    return shouldBeSkipped;
  }

  private static boolean shouldBeSkipped(String path) {
    return path.startsWith("io/opentelemetry");
  }
}
