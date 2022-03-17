/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

class ArchiveEntry {

  private static final ArchiveEntry NOT_CLASS =
      new ArchiveEntry("", "", /* shouldInstrument= */ false);

  private final String name;
  private final String path;
  private final boolean shouldInstrument;

  private ArchiveEntry(String name, String path, boolean shouldInstrument) {
    this.name = name;
    this.path = path;
    this.shouldInstrument = shouldInstrument;
  }

  static ArchiveEntry fromZipEntryName(String zipEntryName) {
    if (!isClass(zipEntryName)) {
      return NOT_CLASS;
    }
    String path = zipEntryName.substring(0, zipEntryName.indexOf(".class"));
    return new ArchiveEntry(className(path), path, !shouldBeSkipped(zipEntryName));
  }

  private static boolean isClass(String path) {
    return path.endsWith(".class");
  }

  private static String className(String path) {
    return path.replace("/", ".");
  }

  private static boolean shouldBeSkipped(String zipEntryName) {
    return zipEntryName.startsWith("io.opentelemetry");
  }

  String getName() {
    return name;
  }

  String getPath() {
    return path;
  }

  boolean shouldInstrument() {
    return shouldInstrument;
  }
}
