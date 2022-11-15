/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.net.URL;
import javax.annotation.Nullable;

interface ResourceLocator {

  @Nullable
  Class<?> findClass(String className);

  URL getClassLocation(Class<?> clazz);
}
