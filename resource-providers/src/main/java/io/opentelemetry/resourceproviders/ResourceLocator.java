/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.resourceproviders;

import java.net.URL;

interface ResourceLocator {

  Class<?> findClass(String className);

  URL getClassLocation(Class<?> clazz);
}
