/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter.agent;

import io.opentelemetry.contrib.staticinstrumenter.util.SystemLogger;
import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Replaces {@link io.opentelemetry.javaagent.OpenTelemetryAgent} and wraps its methods with adding
 * new {@link java.lang.instrument.ClassFileTransformer}s necessary for saving transformed classes.
 */
public final class OpenTelemetryStaticAgent {

  public static void premain(String agentArgs, Instrumentation inst) {
    try {
      installBootstrapJar(inst);
      beforeAgent(inst);
      OpenTelemetryAgent.premain(agentArgs, inst);
      afterAgent(inst);
    } catch (Throwable ex) {
      getLogger().error("Instrumentation failed", ex);
    }
  }

  public static void agentmain(String agentArgs, Instrumentation inst) {
    try {
      installBootstrapJar(inst);
      beforeAgent(inst);
      OpenTelemetryAgent.agentmain(agentArgs, inst);
      afterAgent(inst);
    } catch (Throwable ex) {
      getLogger().error("Instrumentation failed", ex);
    }
  }

  private static void beforeAgent(Instrumentation inst) {
    ClassLoader savedContextClassLoader = Thread.currentThread().getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(null);
      Class<?> clazz = Class.forName("io.opentelemetry.contrib.staticinstrumenter.agent.main.Main");
      Method getPreTransformer = clazz.getMethod("getPreTransformer");
      inst.addTransformer((ClassFileTransformer) getPreTransformer.invoke(null));
    } catch (Throwable e) {
      getLogger().error("Could not configure static instrumenter", e);
    } finally {
      Thread.currentThread().setContextClassLoader(savedContextClassLoader);
    }
  }

  private static void afterAgent(Instrumentation inst) {
    ClassLoader savedContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(null);
      Class<?> clazz = Class.forName("io.opentelemetry.contrib.staticinstrumenter.agent.main.Main");
      Method getPostTransformer = clazz.getMethod("getPostTransformer");
      inst.addTransformer((ClassFileTransformer) getPostTransformer.invoke(null), true);
    } catch (Throwable e) {
      getLogger().error("Could not configure static instrumenter", e);
    } finally {
      Thread.currentThread().setContextClassLoader(savedContextClassLoader);
    }
  }

  private static synchronized File installBootstrapJar(Instrumentation inst)
      throws IOException, URISyntaxException {

    CodeSource codeSource = OpenTelemetryAgent.class.getProtectionDomain().getCodeSource();

    if (codeSource == null) {
      throw new IllegalStateException("could not get agent jar location");
    }

    File javaagentFile = new File(codeSource.getLocation().toURI());

    if (!javaagentFile.isFile()) {
      throw new IllegalStateException(
          "agent jar location doesn't appear to be a file: " + javaagentFile.getAbsolutePath());
    }

    // passing verify false for vendors who sign the agent jar, because jar file signature
    // verification is very slow before the JIT compiler starts up, which on Java 8 is not until
    // after premain execution completes
    JarFile agentJar = new JarFile(javaagentFile, false);
    verifyJarManifestMainClassIsThis(javaagentFile, agentJar);
    inst.appendToBootstrapClassLoaderSearch(agentJar);
    return javaagentFile;
  }

  private static void verifyJarManifestMainClassIsThis(File jarFile, JarFile agentJar)
      throws IOException {
    Manifest manifest = agentJar.getManifest();
    if (manifest.getMainAttributes().getValue("Premain-Class") == null) {
      throw new IllegalStateException(
          "The agent was not installed, because the agent was found in '"
              + jarFile
              + "', which doesn't contain a Premain-Class manifest attribute. Make sure that you"
              + " haven't included the agent jar file inside of an application uber jar.");
    }
  }

  // we shouldn't store any static data in this class
  // so this utility method is used instead of a static logger field
  private static SystemLogger getLogger() {
    return SystemLogger.getLogger(OpenTelemetryStaticAgent.class);
  }

  private OpenTelemetryStaticAgent() {}
}
