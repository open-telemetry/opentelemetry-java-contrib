/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.staticinstrumenter;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.contrib.staticinstrumenter.advices.AgentAdviceClasses;
import io.opentelemetry.contrib.staticinstrumenter.advices.HelperInjectorAdvice;
import io.opentelemetry.contrib.staticinstrumenter.internals.ArchiveEntry;
import io.opentelemetry.contrib.staticinstrumenter.internals.ClassArchive;
import io.opentelemetry.contrib.staticinstrumenter.internals.CurrentClass;
import io.opentelemetry.contrib.staticinstrumenter.internals.Main;
import io.opentelemetry.contrib.staticinstrumenter.internals.PostTransformer;
import io.opentelemetry.contrib.staticinstrumenter.internals.PreTransformer;
import io.opentelemetry.contrib.staticinstrumenter.internals.TransformedClass;
import io.opentelemetry.contrib.staticinstrumenter.util.AgentCreator;
import io.opentelemetry.contrib.staticinstrumenter.util.AgentExtractor;
import io.opentelemetry.contrib.staticinstrumenter.util.TmpDirManager;
import io.opentelemetry.contrib.staticinstrumenter.util.path.IdentityPathGetter;
import io.opentelemetry.contrib.staticinstrumenter.util.path.SimplePathGetter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.DynamicType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for loading and instrumenting the OpenTelemetry javaagent JAR file */
final class AgentInstrumenter {
  private static final Logger logger = LoggerFactory.getLogger(AgentInstrumenter.class);
  private final Map<String, byte[]> classesToInject;
  private final Path otelJar;
  private final Path finalJarDir;

  AgentInstrumenter(String otelJarPath, String finalJarDir) {
    this.classesToInject = new HashMap<>();
    this.otelJar = Path.of(otelJarPath);
    this.finalJarDir = Paths.get(finalJarDir);
  }

  /** Conducts the process of OpenTelemetry javaagent JAR instrumentation */
  void instrument() {
    try {

      TmpDirManager tmpDirManager = new TmpDirManager("static-instrumenter-");

      AgentExtractor agentExtractor = new AgentExtractor(tmpDirManager);
      Path tmpDir = agentExtractor.extractAgent(otelJar);

      URL url = tmpDir.toUri().toURL();
      URLClassLoader otelClassLoader = new URLClassLoader(new URL[] {url});

      Class<?> openTelemetryAgentClass =
          otelClassLoader.loadClass("io.opentelemetry.javaagent.OpenTelemetryAgent");
      Class<?> helperInjectorClass =
          otelClassLoader.loadClass("io.opentelemetry.javaagent.tooling.HelperInjector");

      rebaseOpenTelemetryAgent(openTelemetryAgentClass);
      rebaseHelperInjector(helperInjectorClass);

      prepareStaticInstrumenterClasses();

      new AgentCreator(tmpDirManager, new IdentityPathGetter())
          .create(finalJarDir.resolve("opentelemetry-javaagent.jar"), otelJar, classesToInject);

      new AgentCreator(tmpDirManager, new SimplePathGetter())
          .create(finalJarDir.resolve("classpath-agent.jar"), otelJar, classesToInject);

    } catch (IOException | ClassNotFoundException e) {
      logger.error(
          "The classes required for static instrumentation with OpenTelemetry Agent were not added properly.",
          e);
    }
  }

  /**
   * Modifies the <code>io.opentelemetry.javaagent.OpenTelemetryAgent</code> class so that it adds
   * {@link PreTransformer} and {@link PostTransformer} instances to the {@link
   * java.lang.instrument.Instrumentation} object used in the agent's premain method. Adding a
   * PreTransformer instance must be performed after <code>installBootstrapJar</code> method and
   * adding a PostTransformer must be performed at the end of <code>startAgent</code> method (after
   * initializing the agent).
   */
  private void rebaseOpenTelemetryAgent(Class<?> openTelemetryAgentClass) {

    DynamicType.Unloaded<?> openTelemetryAgentType =
        new ByteBuddy()
            .rebase(openTelemetryAgentClass)
            .visit(
                Advice.to(AgentAdviceClasses.AgentMainAdvice.class)
                    .on(isMethod().and(named("startAgent"))))
            .visit(
                Advice.to(AgentAdviceClasses.InstallBootstrapJarAdvice.class)
                    .on(isMethod().and(named("installBootstrapJar"))))
            .make();

    classesToInject.put(
        openTelemetryAgentType.getTypeDescription().getInternalName() + ".class",
        openTelemetryAgentType.getBytes());
  }

  /**
   * Modifies the <code>io.opentelemetry.javaagent.tooling.HelperInjector</code> class so that it
   * saves additional classes created by the agent in the {@link Main#getAdditionalClasses()} map.
   * At the end of the static instrumentation process, the Main class saves all additionalClasses to
   * the result jar.
   */
  private void rebaseHelperInjector(Class<?> helperInjectorClass) {

    DynamicType.Unloaded<?> helperInjectorType =
        new ByteBuddy()
            .rebase(helperInjectorClass)
            .visit(
                Advice.to(HelperInjectorAdvice.class)
                    .on(isMethod().and(named("injectBootstrapClassLoader"))))
            .make();

    classesToInject.put(
        "inst/" + helperInjectorType.getTypeDescription().getInternalName() + ".classdata",
        helperInjectorType.getBytes());
  }

  private void prepareStaticInstrumenterClasses() {
    List<Class<?>> additionalClasses =
        new ArrayList<>(
            Arrays.asList(
                ArchiveEntry.class,
                ClassArchive.class,
                ClassArchive.Factory.class,
                CurrentClass.class,
                Main.class,
                PreTransformer.class,
                PostTransformer.class,
                TransformedClass.class));

    ByteBuddy byteBuddy = new ByteBuddy();

    for (Class<?> clazz : additionalClasses) {
      DynamicType.Unloaded<?> type = byteBuddy.rebase(clazz).make();

      classesToInject.put(getClassEntryName(type), type.getBytes());
    }
  }

  private static String getClassEntryName(DynamicType.Unloaded<?> unloadedType) {
    return unloadedType.getTypeDescription().getInternalName() + ".class";
  }
}
