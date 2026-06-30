/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.OpampPolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyStore;
import io.opentelemetry.contrib.dynamic.policy.PolicyTypeInitializer;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central registry bootstrap for telemetry policy initialization.
 *
 * <p>This class reads the policy-init configuration that specifies how to wire up the policy
 * pipeline (providers reading policies, eg an OpAMP provider, implementers applying policies, eg a
 * TraceSamplingRatePolicyImplementer), resolves any {@code policyType} strings (eg
 * "trace_sampling_rate_policy") to registered policy classes (eg TraceSamplingRatePolicy),
 * initializes the implementer classes, and activates configured providers that read policies from
 * the source and stream policy updates into the shared {@link PolicyStore}.
 *
 * <p>Generically the pipeline is: message -> provider -> policy -> policy handler -> implementer ->
 * agent config is changed
 *
 * <p>A specific example is: eg "change sampling rate" message -> OpampPolicyProvider ->
 * TraceSamplingRatePolicy -> PolicyStore -> TraceSamplingRatePolicyImplementer -> sampling rate
 * changed
 */
public final class PolicyInit {
  private static final Logger logger = Logger.getLogger(PolicyInit.class.getName());
  private static final Map<String, Class<? extends TelemetryPolicy>> REGISTERED_POLICY_TYPES =
      new ConcurrentHashMap<>();
  private static final Map<Class<? extends TelemetryPolicy>, PolicyTypeInitializer>
      POLICY_TYPE_INITIALIZERS = new ConcurrentHashMap<>();
  private static final AtomicBoolean sourcesActivated = new AtomicBoolean(false);
  private static final Map<Class<? extends TelemetryPolicy>, PolicyImplementer>
      initializedImplementers = new ConcurrentHashMap<>();
  private static final CopyOnWriteArrayList<Closeable> activeSourceWatches =
      new CopyOnWriteArrayList<>();
  private static final Map<PolicyProvider, List<TelemetryPolicy>> sourcePolicies =
      new ConcurrentHashMap<>();
  private static final PolicyStore policyStore = new PolicyStore();
  private static final AtomicReference<PolicyInitConfig> declarativeInitConfig =
      new AtomicReference<>();

  static {
    // For now, policies will be registered here. TODO: move to a more dynamic way.
    TraceSamplingRatePolicy.registerPolicyType();
  }

  /**
   * Registers a policy type string to a concrete policy class and its initializer factory. eg map
   * 'trace-sampling' to the class 'TraceSamplingRatePolicy', and register the initializer factory
   * 'TraceSamplingRatePolicy::initialize' for when the policy is present in the init config.
   *
   * <p>Example:
   *
   * <pre>{@code
   * PolicyInit.registerPolicyType(
   *     TraceSamplingRatePolicy.POLICY_TYPE,
   *     TraceSamplingRatePolicy.class,
   *     TraceSamplingRatePolicy::initialize);
   * }</pre>
   *
   * @param policyType configured policy type identifier (for example {@code trace-sampling})
   * @param policyClass runtime class implementing that policy type
   * @param policyTypeInitializer initializer for the policy type that returns the associated
   *     implementer instance
   * @throws IllegalStateException if the same type is already mapped to a different class
   */
  public static void registerPolicyType(
      String policyType,
      Class<? extends TelemetryPolicy> policyClass,
      PolicyTypeInitializer policyTypeInitializer) {
    Objects.requireNonNull(policyType, "policyType cannot be null");
    Objects.requireNonNull(policyClass, "policyClass cannot be null");
    Objects.requireNonNull(policyTypeInitializer, "policyTypeInitializer cannot be null");
    Class<? extends TelemetryPolicy> existing =
        REGISTERED_POLICY_TYPES.put(policyType, policyClass);
    if (existing != null && !existing.equals(policyClass)) {
      throw new IllegalStateException(
          "Policy type '" + policyType + "' is already registered by " + existing.getName());
    }
    POLICY_TYPE_INITIALIZERS.put(policyClass, policyTypeInitializer);
  }

  /**
   * Installs registry initialization into SDK auto-configuration.
   *
   * <p>When either init-config property is present, this loads JSON or YAML config, resolves and
   * initializes mapped policy classes, and activates runtime policy sources.
   *
   * <p>Example properties:
   *
   * <ul>
   *   <li>{@code otel.java.experimental.telemetry.policy.init.yaml=/path/policy-init.yaml}
   *   <li>{@code otel.java.experimental.telemetry.policy.init.json=/path/policy-init.json}
   * </ul>
   *
   * @param autoConfiguration OpenTelemetry auto-configuration customizer
   */
  public static void init(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesCustomizer(
        config -> {
          PolicyInitConfig initConfig = declarativeInitConfig.getAndSet(null);
          if (initConfig != null) {
            logger.log(
                Level.INFO,
                "Initializing telemetry policies from top-level declarative config with {0} source(s)",
                initConfig.getSources().size());
          } else {
            initConfig = PolicyInitConfig.readFromConfigProperties(config);
          }

          if (initConfig == null) {
            return Collections.emptyMap();
          }
          resolveAndInitializeConfiguredPolicyTypes(initConfig, autoConfiguration);
          activateSources(initConfig, config);
          return Collections.emptyMap();
        });
  }

  /**
   * Stores parsed top-level declarative telemetry policy config for auto-configuration bootstrap.
   */
  public static void setDeclarativeInitConfig(PolicyInitConfig initConfig) {
    declarativeInitConfig.set(Objects.requireNonNull(initConfig, "initConfig cannot be null"));
  }

  /** Initializes dynamic-control policy wiring from declarative config component input. */
  public static void initFromDeclarativeConfig(
      DeclarativeConfigProperties declarativeConfig, ConfigProperties config) {
    Objects.requireNonNull(declarativeConfig, "declarativeConfig cannot be null");
    Objects.requireNonNull(config, "config cannot be null");
    PolicyInitConfig initConfig =
        PolicyInitConfig.readFromTelemetryPolicyDeclarativeConfig(declarativeConfig);
    if (initConfig == null) {
      initConfig = PolicyInitConfig.readFromDeclarativeConfigProperties(declarativeConfig);
    }
    if (initConfig == null) {
      return;
    }
    resolveAndInitializeConfiguredPolicyTypes(initConfig, createNoopAutoConfigurationCustomizer());
    try {
      activateSources(initConfig, config);
    } catch (RuntimeException e) {
      logger.log(
          Level.WARNING,
          "Failed to activate telemetry policy sources from declarative component config",
          e);
    }
  }

  private static AutoConfigurationCustomizer createNoopAutoConfigurationCustomizer() {
    return (AutoConfigurationCustomizer)
        Proxy.newProxyInstance(
            AutoConfigurationCustomizer.class.getClassLoader(),
            new Class<?>[] {AutoConfigurationCustomizer.class},
            (proxy, method, args) -> null);
  }

  /**
   * Resolves all mapped policy types to classes and invokes each policy-type initializer once.
   *
   * <p>eg if the init config has {@code policyType: trace-sampling}, this resolves that policy type
   * to its registered class, TraceSamplingRatePolicy, and runs the registered policy-type
   * initializer for that class, TraceSamplingRatePolicy::initialize.
   *
   * @param initConfig parsed registry initialization model
   * @param autoConfiguration OpenTelemetry auto-configuration customizer
   */
  private static void resolveAndInitializeConfiguredPolicyTypes(
      PolicyInitConfig initConfig, AutoConfigurationCustomizer autoConfiguration) {
    Set<Class<? extends TelemetryPolicy>> initializedPolicyClasses = new HashSet<>();
    for (PolicySourceConfig source : initConfig.getSources()) {
      for (PolicySourceMappingConfig mapping : source.getMappings()) {
        String mappedPolicyType = mapping.getPolicyType();
        Class<? extends TelemetryPolicy> policyClass =
            REGISTERED_POLICY_TYPES.get(mappedPolicyType);
        if (policyClass == null) {
          throw new IllegalArgumentException(
              "Unknown policyType '"
                  + mappedPolicyType
                  + "' in mapping for source kind '"
                  + source.getKind().configValue()
                  + "' key '"
                  + mapping.getSourceKey()
                  + "'");
        }
        initializePolicyClass(policyClass, autoConfiguration, initializedPolicyClasses);
        logger.log(
            Level.INFO,
            "Mapped policyType ''{0}'' to class ''{1}''",
            new Object[] {mappedPolicyType, policyClass.getName()});
      }
    }
  }

  /**
   * Initializes one policy class exactly once until shutdown. eg run {@code
   * TraceSamplingRatePolicy::initialize}, cache the returned implementer, and register it with the
   * policy store.
   *
   * @param policyClass policy class to initialize
   * @param autoConfiguration OpenTelemetry auto-configuration customizer
   * @param initializedPolicyClasses set tracking which classes were already initialized
   */
  private static void initializePolicyClass(
      Class<? extends TelemetryPolicy> policyClass,
      AutoConfigurationCustomizer autoConfiguration,
      Set<Class<? extends TelemetryPolicy>> initializedPolicyClasses) {
    if (!initializedPolicyClasses.add(policyClass)) {
      return;
    }
    PolicyImplementer implementer;
    try {
      synchronized (initializedImplementers) {
        PolicyImplementer existing = initializedImplementers.get(policyClass);
        if (existing != null) {
          return;
        }
        PolicyTypeInitializer policyTypeInitializer = POLICY_TYPE_INITIALIZERS.get(policyClass);
        if (policyTypeInitializer == null) {
          throw new IllegalStateException(
              "No policyTypeInitializer registered for policy class '"
                  + policyClass.getName()
                  + "'");
        }
        implementer =
            Objects.requireNonNull(
                policyTypeInitializer.initialize(autoConfiguration),
                "policyTypeInitializer returned null");
        initializedImplementers.put(policyClass, implementer);
      }
    } catch (RuntimeException e) {
      throw new IllegalStateException(
          "Policy initializer failed for class '" + policyClass.getName() + "'", e);
    }
    policyStore.registerImplementer(implementer);
    logger.log(Level.INFO, "Initialized policy class ''{0}''", policyClass.getName());
  }

  /**
   * Activates runtime sources from the init config and wires policy update propagation. eg activate
   * the OpAMPPolicyProvider to start reading from its source per the init config.
   *
   * <p>This is idempotent; repeated calls after first activation are ignored.
   */
  private static void activateSources(PolicyInitConfig initConfig, ConfigProperties config) {
    if (!sourcesActivated.compareAndSet(false, true)) {
      return;
    }
    for (PolicySourceConfig source : initConfig.getSources()) {
      List<PolicyValidator> validators = createSourceValidators(source);
      if (validators.isEmpty()) {
        logger.log(
            Level.INFO,
            "Skipping source kind ''{0}'' because no validators matched configured mappings",
            source.getKind().configValue());
        continue;
      }
      PolicyProvider provider = source.getKind().createProvider(source, config, validators);
      if (provider == null) {
        logger.log(
            Level.INFO,
            "Skipping source kind ''{0}'' because no provider could be created",
            source.getKind().configValue());
        continue;
      }
      try {
        List<TelemetryPolicy> initialPolicies = provider.fetchPolicies();
        updatePoliciesForSource(provider, initialPolicies);
      } catch (Exception e) {
        logger.log(
            Level.WARNING,
            "Failed to fetch initial policies for provider source ''" + source.getLocation() + "''",
            e);
      }
      Closeable watch =
          provider.startWatching(
              policies -> {
                updatePoliciesForSource(provider, policies);
                logger.log(
                    Level.INFO,
                    "provider source ''{0}'' produced {1} policies",
                    new Object[] {source.getLocation(), policies.size()});
              });
      activeSourceWatches.add(watch);
      logger.log(
          Level.INFO,
          "Activated {0} policy source with location ''{1}''",
          new Object[] {source.getKind().configValue(), source.getLocation()});
    }
  }

  /**
   * Resolves policy validators for a source based on mapped policy classes.
   *
   * <p>For each mapped policy class, this uses the already initialized implementer and aggregates
   * {@link PolicyImplementer#getValidators()}.
   *
   * <p>Example: if a source policy maps to {@code TraceSamplingRatePolicy.class}, and its
   * implementer returns a {@code TraceSamplingValidator}, that validator is included in the source
   * validator list.
   */
  private static List<PolicyValidator> createSourceValidators(PolicySourceConfig source) {
    Set<Class<? extends TelemetryPolicy>> mappedClasses =
        collectMappedPolicyClasses(source.getMappings());
    ArrayList<PolicyValidator> validators = new ArrayList<>();
    for (Class<? extends TelemetryPolicy> policyClass : mappedClasses) {
      PolicyImplementer implementer = initializedImplementers.get(policyClass);
      if (implementer != null) {
        validators.addAll(implementer.getValidators());
      }
    }
    return validators;
  }

  /**
   * Merges source snapshots and updates the effective policy store. This doesn't yet do any useful
   * merging, it just combines them all into a single list.
   *
   * <p>Each source contributes an immutable snapshot; the store receives the flattened union. eg
   * OpAMPPolicyProvider and FilePolicyProvider both produce a new TraceSamplingRatePolicy, this
   * will combine them all into a single list.
   *
   * <p>TODO: implement spec-compliant merge semantics: matching policies' keep values must be
   * combined using the most restrictive result; overlapping policy effects must be commutative,
   * idempotent, and deterministic; duplicate policy IDs across providers must be resolved by
   * provider priority.
   */
  private static void updatePoliciesForSource(
      PolicyProvider provider, List<TelemetryPolicy> policiesFromSource) {
    List<TelemetryPolicy> snapshot =
        policiesFromSource == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(policiesFromSource));
    sourcePolicies.put(provider, snapshot);
    ArrayList<TelemetryPolicy> merged = new ArrayList<>();
    for (List<TelemetryPolicy> policies : sourcePolicies.values()) {
      merged.addAll(policies);
    }
    policyStore.updatePolicies(merged);
  }

  /**
   * Collects mapped policy classes for one source mapping list.
   *
   * <p>Example: if mappings contain policy type {@code trace-sampling} twice, both resolve to
   * {@code TraceSamplingRatePolicy.class}, but the result contains that class only once.
   */
  private static Set<Class<? extends TelemetryPolicy>> collectMappedPolicyClasses(
      List<PolicySourceMappingConfig> mappings) {
    LinkedHashSet<Class<? extends TelemetryPolicy>> result = new LinkedHashSet<>();
    for (PolicySourceMappingConfig mapping : mappings) {
      Class<? extends TelemetryPolicy> policyClass =
          REGISTERED_POLICY_TYPES.get(mapping.getPolicyType());
      if (policyClass != null) {
        result.add(policyClass);
      }
    }
    return result;
  }

  /**
   * Shuts down active policy sources and clears source-activation runtime state.
   *
   * <p>This closes all currently active source watches and allows a subsequent {@link #init}
   * invocation to activate sources again.
   *
   * <p>This does <strong>not</strong> clear global policy type registrations ({@link
   * #registerPolicyType(String, Class, PolicyTypeInitializer)}) and does not replace the static
   * {@link PolicyStore} instance.
   */
  public static void shutdown() {
    for (Closeable watch : activeSourceWatches) {
      try {
        watch.close();
      } catch (IOException e) {
        logger.log(Level.INFO, "Failed to close active source watch during shutdown", e);
      }
    }
    activeSourceWatches.clear();
    sourcePolicies.clear();
    sourcesActivated.set(false);
    initializedImplementers.clear();
    policyStore.clear();
    declarativeInitConfig.set(null);
  }

  /**
   * Resets source-activation runtime state used by tests.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * @AfterEach
   * void tearDown() {
   *   PolicyInit.resetForTest();
   * }
   * }</pre>
   */
  static void resetForTest() {
    shutdown();
    OpampPolicyProvider.resetForTest();
  }

  private PolicyInit() {}
}
