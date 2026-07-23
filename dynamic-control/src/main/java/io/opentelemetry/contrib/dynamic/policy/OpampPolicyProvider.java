/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicySourceMappingConfig;
import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import io.opentelemetry.opamp.client.OpampClient;
import io.opentelemetry.opamp.client.OpampClientBuilder;
import io.opentelemetry.opamp.client.internal.connectivity.http.OkHttpSender;
import io.opentelemetry.opamp.client.internal.request.delay.PeriodicDelay;
import io.opentelemetry.opamp.client.internal.request.service.HttpRequestService;
import io.opentelemetry.opamp.client.internal.response.MessageData;
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.RemoteConfigStatus;
import opamp.proto.RemoteConfigStatuses;
import opamp.proto.ServerErrorResponse;

/**
 * {@link PolicyProvider} implementation backed by OpAMP remote configuration updates.
 *
 * <p>The provider subscribes to an OpAMP endpoint, extracts the configured payload for one source
 * location key, maps incoming policy IDs to internal policy types, validates them with the supplied
 * validators, and publishes the resulting policies to callers.
 */
public final class OpampPolicyProvider extends AbstractPolicyProvider {
  private static final Logger logger = Logger.getLogger(OpampPolicyProvider.class.getName());
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String OPAMP_ENDPOINT = "otel.opamp.service.url";
  private static final String SERVICE_NAME = "otel.service.name";
  private static final String RESOURCE_ATTRIBUTES = "otel.resource.attributes";
  private static final String DEPLOYMENT_ENVIRONMENT_NAME = "deployment.environment.name";
  private static final String DEPLOYMENT_ENVIRONMENT = "deployment.environment";
  private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(30);
  private static final AtomicReference<Duration> GLOBAL_POLLING_INTERVAL =
      new AtomicReference<>(DEFAULT_POLLING_INTERVAL);
  private static final CopyOnWriteArrayList<OpampPolicyProvider> ACTIVE_PROVIDERS =
      new CopyOnWriteArrayList<>();

  private final String endpoint;
  private final String location;
  private final String serviceName;
  @Nullable private final String serviceEnvironment;
  private final Map<String, String> headers;
  private final SourceFormat format;
  private final MappedPolicySourceConverter sourceConverter;
  private final MutablePeriodicDelay pollingDelay;
  private final AtomicReference<OpampClient> clientRef = new AtomicReference<>();
  private final AtomicReference<Thread> shutdownHookRef = new AtomicReference<>();

  /**
   * Creates a provider for one OpAMP-backed policy source without legacy OpAMP headers.
   *
   * @param properties declarative properties used to resolve endpoint/service identity
   * @param configuredLocation source location key used to select one OpAMP config entry
   * @param format payload format parser for the selected source
   * @param mappings policy-id-to-policy-type mappings for this source
   * @param validators validators used to materialize typed {@link TelemetryPolicy} instances
   * @throws IllegalArgumentException if required configuration such as OpAMP endpoint is missing
   */
  public OpampPolicyProvider(
      DeclarativeConfigProperties properties,
      String configuredLocation,
      SourceFormat format,
      List<PolicySourceMappingConfig> mappings,
      List<PolicyValidator> validators) {
    this(PolicyProviderConfig.create(properties), configuredLocation, format, mappings, validators);
  }

  /**
   * Creates a provider with the shared provider configuration context.
   *
   * @param config declarative properties and any legacy OpAMP headers
   * @param configuredLocation source location key used to select one OpAMP config entry
   * @param format payload format parser for the selected source
   * @param mappings policy-id-to-policy-type mappings for this source
   * @param validators validators used to materialize typed {@link TelemetryPolicy} instances
   */
  public OpampPolicyProvider(
      PolicyProviderConfig config,
      String configuredLocation,
      SourceFormat format,
      List<PolicySourceMappingConfig> mappings,
      List<PolicyValidator> validators) {
    Objects.requireNonNull(config, "config cannot be null");
    Objects.requireNonNull(configuredLocation, "configuredLocation cannot be null");
    Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(mappings, "mappings cannot be null");
    Objects.requireNonNull(validators, "validators cannot be null");
    DeclarativeConfigProperties properties = config.getProperties();
    String resolvedEndpoint = getEndpoint(properties);
    if (resolvedEndpoint == null) {
      throw new IllegalArgumentException("Missing OpAMP endpoint property: " + OPAMP_ENDPOINT);
    }
    this.endpoint = resolvedEndpoint;
    this.location = configuredLocation;
    this.serviceName = getServiceName(config);
    this.serviceEnvironment = getServiceEnvironment(config);
    this.headers = config.getOpampHeaders();
    this.format = format;
    this.sourceConverter = MappedPolicySourceConverter.create(mappings, validators);
    this.pollingDelay =
        new MutablePeriodicDelay(
            Objects.requireNonNull(
                GLOBAL_POLLING_INTERVAL.get(), "polling interval cannot be null"));
  }

  /** Returns the latest validated policies received from OpAMP. */
  @Override
  public List<TelemetryPolicy> fetchPolicies() {
    return getCurrentPolicies();
  }

  /**
   * Starts the OpAMP watch loop and registers a callback for policy updates.
   *
   * <p>If already started, this method is idempotent and returns a handle that still stops the
   * active watcher.
   *
   * @param onUpdate callback invoked with an immutable snapshot whenever policies change
   * @return a {@link Closeable} that stops watching
   */
  @Override
  public Closeable startWatching(Consumer<List<TelemetryPolicy>> onUpdate) {
    Objects.requireNonNull(onUpdate, "onUpdate cannot be null");
    OpampClient existing = clientRef.get();
    if (existing != null) {
      return this::stop;
    }

    headers.forEach((key, value) -> logger.info("OpAMP header: " + key));

    logger.info("Starting OpAMP client for: " + serviceName + " on endpoint " + endpoint);
    OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient().newBuilder();
    okHttpClientBuilder
        .interceptors()
        .add(
            chain -> {
              Request.Builder modifiedRequest = chain.request().newBuilder();
              headers.forEach(
                  (key, value) -> {
                    if (value != null) {
                      modifiedRequest.addHeader(key, value);
                    }
                  });
              return chain.proceed(modifiedRequest.build());
            });
    HttpRequestService requestService =
        HttpRequestService.create(
            OkHttpSender.create(endpoint, okHttpClientBuilder.build()),
            pollingDelay,
            HttpRequestService.DEFAULT_DELAY_BETWEEN_RETRIES);

    OpampClientBuilder builder =
        OpampClient.builder()
            .setRequestService(requestService)
            .enableRemoteConfig()
            .putIdentifyingAttribute("service.name", serviceName);
    if (serviceEnvironment != null && !serviceEnvironment.isEmpty()) {
      builder.putNonIdentifyingAttribute(DEPLOYMENT_ENVIRONMENT_NAME, serviceEnvironment);
    }
    OpampClient client =
        builder.build(
            new OpampClient.Callbacks() {
              @Override
              public void onConnect(OpampClient client) {}

              @Override
              public void onConnectFailed(OpampClient client, @Nullable Throwable throwable) {
                if (throwable == null) {
                  logger.info("OpAMP connection failed");
                } else {
                  logger.info("OpAMP connection failed: " + throwable.getMessage());
                }
              }

              @Override
              public void onErrorResponse(OpampClient client, ServerErrorResponse errorResponse) {
                logger.info("OpAMP server error: " + errorResponse.error_message);
              }

              @Override
              public void onMessage(OpampClient client, MessageData messageData) {
                RemoteConfigStatus status = handleMessage(messageData, onUpdate);
                if (status != null) {
                  client.setRemoteConfigStatus(status);
                }
              }
            });

    if (!clientRef.compareAndSet(null, client)) {
      safeClose(client);
      return this::stop;
    }
    ACTIVE_PROVIDERS.add(this);

    Thread shutdownHook = new Thread(this::stop, "opamp-policy-provider-shutdown");
    shutdownHookRef.set(shutdownHook);
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    return this::stop;
  }

  @Nullable
  private RemoteConfigStatus handleMessage(
      MessageData messageData, Consumer<List<TelemetryPolicy>> onUpdate) {
    AgentRemoteConfig remoteConfig = messageData.getRemoteConfig();
    if (remoteConfig == null) {
      return null;
    }
    AgentConfigMap configMap = remoteConfig.config;
    if (configMap == null || configMap.config_map == null || configMap.config_map.isEmpty()) {
      updateCurrentPoliciesAndNotify(Collections.<TelemetryPolicy>emptyList(), onUpdate);
      return buildStatus(
          RemoteConfigStatuses.RemoteConfigStatuses_FAILED, remoteConfig.config_hash);
    }
    AgentConfigFile selected = configMap.config_map.get(location);
    if (selected == null || selected.body == null) {
      logger.info("No OpAMP config payload found for location key: " + location);
      updateCurrentPoliciesAndNotify(Collections.<TelemetryPolicy>emptyList(), onUpdate);
      return buildStatus(
          RemoteConfigStatuses.RemoteConfigStatuses_FAILED, remoteConfig.config_hash);
    }

    List<TelemetryPolicy> policies = new ArrayList<>();
    parsePolicyText(location, selected.body.utf8(), policies);
    List<TelemetryPolicy> snapshot = updateCurrentPoliciesAndNotify(policies, onUpdate);
    RemoteConfigStatuses status =
        snapshot.isEmpty()
            ? RemoteConfigStatuses.RemoteConfigStatuses_FAILED
            : RemoteConfigStatuses.RemoteConfigStatuses_APPLIED;
    return buildStatus(status, remoteConfig.config_hash);
  }

  private void parsePolicyText(String key, String policyText, List<TelemetryPolicy> out) {
    logger.info("Received OpAMP policy payload for key '" + key + "': " + policyText);
    List<SourceWrapper> parsedSources = format.parse(policyText);
    if (parsedSources == null && format == SourceFormat.JSONKEYVALUE) {
      parsedSources = parseMappedJsonObject(policyText, sourceConverter.getMappedPolicyIds());
    }
    if (parsedSources == null) {
      logger.info("Ignoring invalid OpAMP config entry for key: " + key);
      return;
    }
    out.addAll(sourceConverter.convert(parsedSources, SourceKind.OPAMP));
  }

  private void stop() {
    OpampClient client = clientRef.getAndSet(null);
    if (client != null) {
      logger.info("Shutting down OpAMP client for: " + serviceName);
      safeClose(client);
    }
    ACTIVE_PROVIDERS.remove(this);
    Thread shutdownHook = shutdownHookRef.getAndSet(null);
    if (shutdownHook != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException ignored) {
        // JVM is shutting down.
      }
    }
  }

  private static void safeClose(OpampClient client) {
    try {
      client.close();
    } catch (IOException e) {
      logger.info("Error during OpAMP shutdown: " + e.getMessage());
    }
  }

  /**
   * Sets the global polling interval used by all active and future providers.
   *
   * @param interval new polling interval, must be greater than zero
   * @throws IllegalArgumentException if interval is zero or negative
   */
  public static void setGlobalPollingInterval(Duration interval) {
    Objects.requireNonNull(interval, "interval cannot be null");
    if (interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("interval must be > 0");
    }
    GLOBAL_POLLING_INTERVAL.set(interval);
    for (OpampPolicyProvider provider : ACTIVE_PROVIDERS) {
      provider.setPollingInterval(interval);
    }
  }

  /** Returns the current global polling interval for unit tests. */
  static Duration getGlobalPollingIntervalForTest() {
    return Objects.requireNonNull(
        GLOBAL_POLLING_INTERVAL.get(), "global polling interval cannot be null");
  }

  /** Resets shared provider test state, including polling interval and active provider tracking. */
  // TODO: refactor/rescope to make this not public at some point
  public static void resetForTest() {
    setGlobalPollingInterval(DEFAULT_POLLING_INTERVAL);
    ACTIVE_PROVIDERS.clear();
  }

  void setPollingInterval(Duration interval) {
    pollingDelay.setDelay(interval);
    logger.info("Updated OpAMP polling interval to " + interval);
  }

  /**
   * Resolves and normalizes the OpAMP endpoint URL from configuration.
   *
   * <p>Returns {@code null} when unset.
   */
  @Nullable
  static String getEndpoint(DeclarativeConfigProperties properties) {
    String endpoint = properties.getString(OPAMP_ENDPOINT);
    if (endpoint == null || endpoint.isEmpty()) {
      return null;
    }
    return normalizeEndpoint(endpoint);
  }

  /**
   * Resolves service name from configuration.
   *
   * <p>Resolution order: {@code otel.service.name}, then {@code service.name} from {@code
   * otel.resource.attributes}, then {@code unknown_service:java}.
   */
  static String getServiceName(PolicyProviderConfig config) {
    DeclarativeConfigProperties properties = config.getProperties();
    String configuredServiceName = properties.getString(SERVICE_NAME);
    if (configuredServiceName != null) {
      return configuredServiceName;
    }
    String resourceServiceName = getResourceAttribute(config, "service.name");
    if (resourceServiceName != null) {
      return resourceServiceName;
    }
    return "unknown_service:java";
  }

  /**
   * Resolves deployment environment from resource attributes.
   *
   * <p>Resolution order: {@code deployment.environment.name}, then {@code deployment.environment}.
   */
  @Nullable
  static String getServiceEnvironment(PolicyProviderConfig config) {
    String semconvEnvironment = getResourceAttribute(config, DEPLOYMENT_ENVIRONMENT_NAME);
    if (semconvEnvironment != null) {
      return semconvEnvironment;
    }
    return getResourceAttribute(config, DEPLOYMENT_ENVIRONMENT);
  }

  @Nullable
  private static String getResourceAttribute(PolicyProviderConfig config, String name) {
    String value = config.getResourceAttributes().get(name);
    if (value != null) {
      return value;
    }
    return config.getProperties().get(RESOURCE_ATTRIBUTES).getString(name);
  }

  private static String normalizeEndpoint(String endpoint) {
    String trimmed = endpoint.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("OpAMP endpoint cannot be empty");
    }
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("OpAMP endpoint cannot be empty");
    }
    if (!trimmed.endsWith("v1/opamp")) {
      trimmed += "/v1/opamp";
    }
    return trimmed;
  }

  @Nullable
  private static List<SourceWrapper> parseMappedJsonObject(
      String policyText, Set<String> allowedKeys) {
    try {
      JsonNode root = MAPPER.readTree(policyText);
      if (!root.isObject()) {
        return null;
      }
      List<SourceWrapper> wrappers = new ArrayList<>();
      for (String key : allowedKeys) {
        JsonNode value = root.get(key);
        if (value == null) {
          continue;
        }
        ObjectNode singlePolicy = MAPPER.createObjectNode();
        singlePolicy.set(key, value);
        wrappers.add(new JsonSourceWrapper(singlePolicy));
      }
      return wrappers;
    } catch (IOException e) {
      return null;
    }
  }

  private static RemoteConfigStatus buildStatus(
      RemoteConfigStatuses status, @Nullable ByteString hash) {
    if (hash != null && status == RemoteConfigStatuses.RemoteConfigStatuses_APPLIED) {
      return new RemoteConfigStatus.Builder().status(status).last_remote_config_hash(hash).build();
    }
    return new RemoteConfigStatus.Builder().status(status).build();
  }

  private static final class MutablePeriodicDelay implements PeriodicDelay {
    private final AtomicReference<Duration> delay = new AtomicReference<>();

    private MutablePeriodicDelay(Duration initialDelay) {
      setDelay(initialDelay);
    }

    @Override
    public Duration getNextDelay() {
      return Objects.requireNonNull(delay.get(), "delay cannot be null");
    }

    @Override
    public void reset() {}

    private void setDelay(Duration value) {
      Objects.requireNonNull(value, "value cannot be null");
      if (value.isZero() || value.isNegative()) {
        throw new IllegalArgumentException("delay must be > 0");
      }
      delay.set(value);
    }
  }
}
