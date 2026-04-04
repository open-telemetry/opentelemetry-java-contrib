/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfrevent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jdk.jfr.Label;
import jdk.jfr.MetadataDefinition;
import jdk.jfr.Name;

/**
 * Meta-annotation to support @Contextual without compiling against JDK 25.
 *
 * @see <a href="https://bugs.openjdk.org/browse/JDK-8356699">JDK-8356699</a>
 */
@MetadataDefinition
@Label("Context")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Name("jdk.jfr.Contextual")
@interface Contextual {}
