/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Shared polling loop for providers that need periodic source change checks.
 *
 * <p>Polling interval can be set using the system property {@link #POLL_INTERVAL_PROPERTY}. eg
 * -Dotel.java.experimental.telemetry.policy.provider.poll.interval=30s The default poll interval is
 * 30 seconds.
 *
 * <p>The polling loop is shared by all registered targets and is started when the first target is
 * registered and stopped when the last target is unregistered.
 *
 * <p>Of the three source types, opamp, http and file, the http and file sources are polled using
 * this shared scheduler. The opamp source is polled using a scheduler built into the opamp
 * implementation.
 *
 * <p>As a defensive measure, the http response body is limited to 4MB
 * (MAX_URL_RESPONSE_BODY_BYTES). If the response body exceeds 4MB, the poller will log an
 * exception.
 */
@SuppressWarnings("NonFinalStaticField")
public final class PolicyProviderPoller {
  private static final Logger logger = Logger.getLogger(PolicyProviderPoller.class.getName());

  // duration, eg -Dotel.java.experimental.telemetry.policy.provider.poll.interval=30s
  public static final String POLL_INTERVAL_PROPERTY =
      "otel.java.experimental.telemetry.policy.provider.poll.interval";
  private static final int URL_READ_TIMEOUT_MILLIS = 10_000;
  private static final int MAX_URL_RESPONSE_BODY_BYTES = 4 * 1024 * 1024;
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(30);
  private static final AtomicReference<Duration> GLOBAL_POLL_INTERVAL =
      new AtomicReference<>(DEFAULT_POLL_INTERVAL);
  private static final CopyOnWriteArrayList<PollingTarget> ACTIVE_TARGETS =
      new CopyOnWriteArrayList<>();
  private static final Object SCHEDULER_LOCK = new Object();

  @Nullable private static ScheduledExecutorService scheduler;
  @Nullable private static ScheduledFuture<?> scheduledTask;
  @Nullable private static Thread shutdownHook;
  private static int schedulerStartCount;

  /** Callback invoked after a monitored file changes. */
  @FunctionalInterface
  public interface FilePollingTarget {
    /**
     * Handles one file change detected by the shared poller.
     *
     * @param file file whose state changed
     * @throws Exception if handling the file change fails
     */
    void onModified(Path file) throws Exception;
  }

  /** Callback invoked after a monitored URL response changes. */
  @FunctionalInterface
  public interface UrlPollingTarget {
    /**
     * Handles one URL response change detected by the shared poller.
     *
     * @param url URL whose response changed
     * @param responseBody response body from the request that detected the change
     * @throws Exception if handling the URL response change fails
     */
    void onModified(URI url, byte[] responseBody) throws Exception;
  }

  @FunctionalInterface
  private interface PollingTarget {
    void poll() throws Exception;
  }

  private static Closeable register(PollingTarget target) {
    Objects.requireNonNull(target, "target cannot be null");
    ACTIVE_TARGETS.add(target);
    startSharedScheduler();
    return () -> unregister(target);
  }

  /**
   * Registers a file-backed target that is invoked only after the file state changes.
   *
   * <p>The initial file state is captured at registration time. Later poll ticks compare the
   * current state against that snapshot and invoke {@code onModified} only when the file is
   * created, deleted, or its timestamp/size changes.
   *
   * @param file file to monitor for changes
   * @param onModified callback invoked after a file-state change is detected
   * @return handle that unregisters the file target when closed
   */
  public static Closeable registerFile(Path file, FilePollingTarget onModified) {
    Objects.requireNonNull(file, "file cannot be null");
    Objects.requireNonNull(onModified, "onModified cannot be null");
    return register(new FilePollingRegistration(file, onModified));
  }

  /**
   * Registers an HTTP(S)-backed target that is invoked only after the URL response changes.
   *
   * <p>The initial response state is captured at registration time. Later poll ticks use
   * conditional request headers when possible and invoke {@code onModified} only when the response
   * status, validators, or body hash changes.
   *
   * @param url HTTP or HTTPS URL to monitor for changes
   * @param onModified callback invoked after a URL response change is detected
   * @return handle that unregisters the URL target when closed
   * @throws IllegalArgumentException if the URI scheme is not {@code http} or {@code https}
   */
  public static Closeable registerUrl(URI url, UrlPollingTarget onModified) {
    Objects.requireNonNull(url, "url cannot be null");
    Objects.requireNonNull(onModified, "onModified cannot be null");
    validateHttpUrl(url);
    return register(new UrlPollingRegistration(url, onModified));
  }

  /**
   * Sets the shared poll interval used by all active and future poll targets.
   *
   * @param interval new poll interval, must be greater than zero
   * @throws IllegalArgumentException if interval is zero or negative
   */
  public static void setGlobalPollInterval(Duration interval) {
    Objects.requireNonNull(interval, "interval cannot be null");
    if (interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("interval must be > 0");
    }
    GLOBAL_POLL_INTERVAL.set(interval);
    synchronized (SCHEDULER_LOCK) {
      if (scheduledTask != null && !scheduledTask.isCancelled()) {
        scheduledTask.cancel(false);
        scheduledTask = scheduleTask(Objects.requireNonNull(scheduler, "scheduler cannot be null"));
      }
    }
  }

  /**
   * Returns the current shared poll interval.
   *
   * @return current poll interval
   */
  public static Duration getGlobalPollInterval() {
    return Objects.requireNonNull(
        GLOBAL_POLL_INTERVAL.get(), "global poll interval cannot be null");
  }

  /**
   * Returns the number of currently registered poll targets.
   *
   * @return active target count
   */
  public static int getActiveTargetCount() {
    return ACTIVE_TARGETS.size();
  }

  /**
   * Returns how many times the shared scheduler has been started since the last reset.
   *
   * @return scheduler start count
   */
  public static int getSchedulerStartCount() {
    synchronized (SCHEDULER_LOCK) {
      return schedulerStartCount;
    }
  }

  /**
   * Returns whether the shared scheduler is currently running.
   *
   * @return true if the scheduler is active, otherwise false
   */
  public static boolean isSchedulerRunning() {
    synchronized (SCHEDULER_LOCK) {
      return scheduler != null && !scheduler.isShutdown();
    }
  }

  /** Runs one immediate poll pass over all currently registered targets. */
  public static void poll() {
    pollTargets();
  }

  /**
   * Stops the shared scheduler and clears all registered targets and poller state.
   *
   * <p>This is intended for controlled shutdown and test cleanup.
   */
  public static void reset() {
    synchronized (SCHEDULER_LOCK) {
      stopSharedSchedulerLocked();
      ACTIVE_TARGETS.clear();
      GLOBAL_POLL_INTERVAL.set(DEFAULT_POLL_INTERVAL);
      schedulerStartCount = 0;
    }
  }

  private static void unregister(PollingTarget target) {
    ACTIVE_TARGETS.remove(target);
    synchronized (SCHEDULER_LOCK) {
      if (ACTIVE_TARGETS.isEmpty()) {
        stopSharedSchedulerLocked();
      }
    }
  }

  private static void startSharedScheduler() {
    synchronized (SCHEDULER_LOCK) {
      if (scheduler != null && !scheduler.isShutdown()) {
        return;
      }
      scheduler = Executors.newSingleThreadScheduledExecutor(new PollerThreadFactory());
      scheduledTask = scheduleTask(scheduler);
      schedulerStartCount++;
      Thread hook =
          new Thread(
              PolicyProviderPoller::shutdownSharedScheduler, "policy-provider-poller-shutdown");
      shutdownHook = hook;
      Runtime.getRuntime().addShutdownHook(hook);
    }
  }

  private static ScheduledFuture<?> scheduleTask(ScheduledExecutorService executor) {
    Duration interval =
        Objects.requireNonNull(GLOBAL_POLL_INTERVAL.get(), "global poll interval cannot be null");
    long delayMillis = Math.max(1, interval.toMillis());
    return executor.scheduleWithFixedDelay(
        PolicyProviderPoller::pollTargets, delayMillis, delayMillis, TimeUnit.MILLISECONDS);
  }

  private static void pollTargets() {
    for (PollingTarget target : ACTIVE_TARGETS) {
      try {
        target.poll();
      } catch (Exception e) {
        logger.log(Level.WARNING, "Unexpected error polling policy provider target", e);
      }
    }
  }

  private static void shutdownSharedScheduler() {
    synchronized (SCHEDULER_LOCK) {
      stopSharedSchedulerLocked();
      ACTIVE_TARGETS.clear();
    }
  }

  private static void stopSharedSchedulerLocked() {
    if (scheduledTask != null) {
      scheduledTask.cancel(false);
      scheduledTask = null;
    }
    if (scheduler != null) {
      scheduler.shutdownNow();
      scheduler = null;
    }
    if (shutdownHook != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException ignored) {
        // JVM is shutting down.
      }
      shutdownHook = null;
    }
  }

  private static final class PollerThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "policy-provider-poller");
      thread.setDaemon(true);
      return thread;
    }
  }

  private static final class FilePollingRegistration implements PollingTarget {
    private final Path file;
    private final FilePollingTarget onModified;
    private final AtomicReference<FileState> lastKnownFileState;

    private FilePollingRegistration(Path file, FilePollingTarget onModified) {
      this.file = file;
      this.onModified = onModified;
      this.lastKnownFileState = new AtomicReference<>(readFileState(file));
    }

    @Override
    public void poll() throws Exception {
      FileState currentFileState = readFileState(file);
      FileState previousFileState =
          Objects.requireNonNull(lastKnownFileState.get(), "lastKnownFileState cannot be null");
      if (currentFileState.equals(previousFileState)) {
        return;
      }
      onModified.onModified(file);
      // Update only after successful handling so a callback failure is retried on the next poll.
      lastKnownFileState.set(currentFileState);
    }
  }

  private static FileState readFileState(Path file) {
    try {
      if (!Files.exists(file)) {
        return FileState.missing();
      }
      return new FileState(
          /* exists= */ true, Files.getLastModifiedTime(file).toMillis(), Files.size(file));
    } catch (NoSuchFileException e) {
      return FileState.missing();
    } catch (IOException e) {
      logger.log(Level.INFO, "Failed to read policy provider file state: " + file, e);
      return FileState.unreadable();
    }
  }

  private static void validateHttpUrl(URI url) {
    String scheme = url.getScheme();
    if (scheme == null) {
      throw new IllegalArgumentException("URL must use http or https scheme");
    }
    String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
      throw new IllegalArgumentException("URL must use http or https scheme");
    }
  }

  @Nullable
  private static UrlPollResult readUrl(URI url, @Nullable UrlState previousState) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url.toURL().openConnection();
      connection.setConnectTimeout(URL_READ_TIMEOUT_MILLIS);
      connection.setReadTimeout(URL_READ_TIMEOUT_MILLIS);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestMethod("GET");
      if (previousState != null) {
        if (previousState.etag.isPresent()) {
          connection.setRequestProperty("If-None-Match", previousState.etag.get());
        }
        if (previousState.lastModified.isPresent()) {
          connection.setRequestProperty("If-Modified-Since", previousState.lastModified.get());
        }
      }
      int statusCode = connection.getResponseCode();
      if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED && previousState != null) {
        return new UrlPollResult(previousState, new byte[0]);
      }
      byte[] responseBody = readResponseBody(connection);
      return new UrlPollResult(
          new UrlState(
              statusCode,
              Optional.ofNullable(connection.getHeaderField("ETag")),
              Optional.ofNullable(connection.getHeaderField("Last-Modified")),
              Arrays.hashCode(responseBody)),
          responseBody);
    } catch (IOException e) {
      logger.log(Level.INFO, "Failed to read policy provider URL state: " + url, e);
      return null;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @SuppressWarnings("TryWithResourcesVariable")
  private static byte[] readResponseBody(HttpURLConnection connection) throws IOException {
    InputStream stream =
        connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST
            ? connection.getErrorStream()
            : connection.getInputStream();
    if (stream == null) {
      return new byte[0];
    }
    try (InputStream in = stream;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        if (out.size() + read > MAX_URL_RESPONSE_BODY_BYTES) {
          throw new IOException(
              "Policy provider URL response body exceeds "
                  + MAX_URL_RESPONSE_BODY_BYTES
                  + " bytes");
        }
        out.write(buffer, 0, read);
      }
      return out.toByteArray();
    }
  }

  private static final class FileState {
    private static final FileState MISSING = new FileState(/* exists= */ false, -1, -1);
    private static final FileState UNREADABLE = new FileState(/* exists= */ false, -2, -1);

    private final boolean exists;
    private final long lastModifiedMillis;
    private final long size;

    private FileState(boolean exists, long lastModifiedMillis, long size) {
      this.exists = exists;
      this.lastModifiedMillis = lastModifiedMillis;
      this.size = size;
    }

    private static FileState missing() {
      return MISSING;
    }

    private static FileState unreadable() {
      return UNREADABLE;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof FileState)) {
        return false;
      }
      FileState that = (FileState) obj;
      return exists == that.exists
          && lastModifiedMillis == that.lastModifiedMillis
          && size == that.size;
    }

    @Override
    public int hashCode() {
      return Objects.hash(exists, lastModifiedMillis, size);
    }
  }

  private static final class UrlPollingRegistration implements PollingTarget {
    private final URI url;
    private final UrlPollingTarget onModified;
    private final AtomicReference<UrlState> lastKnownUrlState;

    private UrlPollingRegistration(URI url, UrlPollingTarget onModified) {
      this.url = url;
      this.onModified = onModified;
      UrlPollResult initialResult = readUrl(url, null);
      this.lastKnownUrlState =
          new AtomicReference<>(initialResult == null ? null : initialResult.state);
    }

    @Override
    public void poll() throws Exception {
      UrlState previousUrlState = lastKnownUrlState.get();
      UrlPollResult currentResult = readUrl(url, previousUrlState);
      if (currentResult == null) {
        return;
      }
      UrlState currentUrlState = currentResult.state;
      if (previousUrlState != null && currentUrlState.equals(previousUrlState)) {
        return;
      }
      onModified.onModified(url, currentResult.responseBody);
      // Update only after successful handling so a callback failure is retried on the next poll.
      lastKnownUrlState.set(currentUrlState);
    }
  }

  private static final class UrlPollResult {
    private final UrlState state;
    private final byte[] responseBody;

    private UrlPollResult(UrlState state, byte[] responseBody) {
      this.state = state;
      this.responseBody = responseBody;
    }
  }

  private static final class UrlState {
    private final int statusCode;
    private final Optional<String> etag;
    private final Optional<String> lastModified;
    private final int bodyHash;

    private UrlState(
        int statusCode, Optional<String> etag, Optional<String> lastModified, int bodyHash) {
      this.statusCode = statusCode;
      this.etag = etag;
      this.lastModified = lastModified;
      this.bodyHash = bodyHash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof UrlState)) {
        return false;
      }
      UrlState that = (UrlState) obj;
      return statusCode == that.statusCode
          && bodyHash == that.bodyHash
          && etag.equals(that.etag)
          && lastModified.equals(that.lastModified);
    }

    @Override
    public int hashCode() {
      return Objects.hash(statusCode, etag, lastModified, bodyHash);
    }
  }

  private PolicyProviderPoller() {}
}
