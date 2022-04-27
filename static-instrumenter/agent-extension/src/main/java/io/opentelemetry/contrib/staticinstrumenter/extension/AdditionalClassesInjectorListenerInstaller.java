/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.extension;

import io.opentelemetry.contrib.staticinstrumenter.agent.main.AdditionalClasses;
import io.opentelemetry.javaagent.tooling.HelperInjectorListener;
import java.util.Map;

/**
 * A listener to be registered in {@link io.opentelemetry.javaagent.tooling.HelperInjector}. It
 * saves all additional classes created by the agent to the AdditionalClasses class.
 */
public class AdditionalClassesInjectorListenerInstaller implements HelperInjectorListener {

  @Override
  public void onInjection(Map<String, byte[]> classnameToBytes) {
    for (Map.Entry<String, byte[]> classEntry : classnameToBytes.entrySet()) {
      String classFileName = classEntry.getKey().replace(".", "/") + ".class";
      AdditionalClasses.put(classFileName, classEntry.getValue());
    }
  }
}
