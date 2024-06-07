/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.contrib.inferredspans.asyncprofiler.JfrParser;
import io.opentelemetry.contrib.inferredspans.collections.Long2ObjectHashMap;
import io.opentelemetry.contrib.inferredspans.config.WildcardMatcher;
import io.opentelemetry.contrib.inferredspans.pooling.Allocator;
import io.opentelemetry.contrib.inferredspans.pooling.ObjectPool;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import one.profiler.AsyncProfiler;

/**
 * Correlates {@link ActivationEvent}s with {@link StackFrame}s which are recorded by {@link
 * AsyncProfiler}, a native <a
 * href="http://psy-lob-saw.blogspot.com/2016/06/the-pros-and-cons-of-agct.html">{@code
 * AsyncGetCallTree}</a>-based (and therefore <a
 * href="http://psy-lob-saw.blogspot.com/2016/02/why-most-sampling-java-profilers-are.html">non
 * safepoint-biased</a>) JVMTI agent.
 *
 * <p>Recording of {@link ActivationEvent}s:
 *
 * <p>The {@link #onActivation} and {@link #onDeactivation} methods are called by {@link
 * ProfilingActivationListener} which register an {@link ActivationEvent} to a {@linkplain
 * #eventBuffer ring buffer} whenever a {@link Span} gets {@link Span#activate()}d or {@link
 * Span#deactivate()}d while a {@linkplain #profilingSessionOngoing profiling session is ongoing}. A
 * background thread consumes the {@link ActivationEvent}s and writes them to a {@linkplain
 * #activationEventsBuffer direct buffer} which is flushed to a {@linkplain
 * #activationEventsFileChannel file}. That is necessary because within a profiling session (which
 * lasts 10s by default) there may be many more {@link ActivationEvent}s than the ring buffer {@link
 * #RING_BUFFER_SIZE can hold}. The file can hold {@link #ACTIVATION_EVENTS_IN_FILE} events and each
 * is {@link ActivationEvent#SERIALIZED_SIZE} in size. This process is completely garbage free
 * thanks to the {@link RingBuffer} acting as an object pool for {@link ActivationEvent}s.
 *
 * <p>Recording stack traces:
 *
 * <p>The same background thread that processes the {@link ActivationEvent}s starts the wall clock
 * profiler of async-profiler via {@link AsyncProfiler#execute(String)}. After the {@link
 * InferredSpansConfiguration#getProfilingDuration()} is over it stops the profiling and starts
 * processing the JFR file created by async-profiler with {@link JfrParser}.
 *
 * <p>Correlating {@link ActivationEvent}s with the traces recorded by {@link AsyncProfiler}:
 *
 * <p>After both the JFR file and the file containing the {@link ActivationEvent}s have been
 * written, it's now time to process them in tandem by correlating based on thread ids and
 * timestamps. The result of this correlation, performed by {@link #processTraces}, are {@link
 * CallTree}s which are created for each thread which has seen an {@linkplain Span#activate()
 * activation} and at least one stack trace. Once {@linkplain
 * ActivationEvent#handleDeactivationEvent(SamplingProfiler) handling the deactivation event} of the
 * root span in a thread (after which {@link ElasticApmTracer#getActive()} would return {@code
 * null}), the {@link CallTree} is {@linkplain CallTree#spanify(CallTree.Root, Span, TraceContext,
 * SpanAnchoredClock, StringBuilder, Tracer)} converted into regular spans}.
 *
 * <p>Overall, the allocation rate does not depend on the number of {@link ActivationEvent}s but
 * only on {@link InferredSpansConfiguration#getProfilingInterval()} and {@link
 * InferredSpansConfiguration#getSamplingInterval()}. Having said that, there are some optimizations
 * so that the JFR file is not processed at all if there have not been any {@link ActivationEvent}
 * in a given profiling session. Also, only if there's a {@link CallTree.Root} for a {@link
 * StackTraceEvent}, we will {@link JfrParser#resolveStackTrace(long, List, int) resolve the full
 * stack trace}.
 */
class SamplingProfiler implements Runnable {

  private static final String LIB_DIR_PROPERTY_NAME = "one.profiler.extractPath";

  private static final Logger logger = Logger.getLogger(SamplingProfiler.class.getName());
  private static final int ACTIVATION_EVENTS_IN_FILE = 1_000_000;
  private static final int MAX_STACK_DEPTH = 256;
  private static final int PRE_ALLOCATE_ACTIVATION_EVENTS_FILE_MB = 10;
  private static final int MAX_ACTIVATION_EVENTS_FILE_SIZE =
      ACTIVATION_EVENTS_IN_FILE * ActivationEvent.SERIALIZED_SIZE;
  private static final int ACTIVATION_EVENTS_BUFFER_SIZE =
      ActivationEvent.SERIALIZED_SIZE * 4 * 1024;
  private final SpanAnchoredClock clock;
  private final EventTranslatorTwoArg<ActivationEvent, Span, Span> activationEventTranslator;

  private final EventTranslatorTwoArg<ActivationEvent, Span, Span> deactivationEventTranslator;
  static final int RING_BUFFER_SIZE = 4 * 1024;

  // Visible for testing
  final InferredSpansConfiguration config;
  private final ScheduledExecutorService scheduler;
  private final Long2ObjectHashMap<CallTree.Root> profiledThreads = new Long2ObjectHashMap<>();
  private final RingBuffer<ActivationEvent> eventBuffer;
  private volatile boolean profilingSessionOngoing = false;
  private final Sequence sequence;
  private final ObjectPool<CallTree.Root> rootPool;
  private final ThreadMatcher threadMatcher = new ThreadMatcher();
  private final EventPoller<ActivationEvent> poller;
  @Nullable private File jfrFile;
  private boolean canDeleteJfrFile;
  private final WriteActivationEventToFileHandler writeActivationEventToFileHandler =
      new WriteActivationEventToFileHandler();
  @Nullable private JfrParser jfrParser;
  private volatile int profilingSessions;

  private final ByteBuffer activationEventsBuffer;

  /**
   * Used to efficiently write {@link #activationEventsBuffer} via {@link
   * FileChannel#write(ByteBuffer)}
   */
  @Nullable private File activationEventsFile;

  private boolean canDeleteActivationEventsFile;

  @Nullable private FileChannel activationEventsFileChannel;
  private final ObjectPool<CallTree> callTreePool;
  private final TraceContext contextForLogging;

  private final ProfilingActivationListener activationListener;

  private final Supplier<Tracer> tracerProvider;

  private final AsyncProfiler profiler;

  /**
   * Creates a sampling profiler, optionally relying on existing files.
   *
   * <p>This constructor is most likely used for tests that rely on a known set of files
   *
   * @param config configuration
   * @param nanoClock clock
   * @param tracerProvider the tracer to use for producing spans
   * @param activationEventsFile activation events file, if {@literal null} a temp file will be used
   * @param jfrFile java flight recorder file, if {@literal null} a temp file will be used instead
   */
  SamplingProfiler(
      InferredSpansConfiguration config,
      SpanAnchoredClock nanoClock,
      Supplier<Tracer> tracerProvider,
      @Nullable File activationEventsFile,
      @Nullable File jfrFile) {
    this.config = config;
    this.tracerProvider = tracerProvider;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread thread = new Thread(r);
              thread.setDaemon(true);
              thread.setName("otel-inferred-spans");
              return thread;
            });
    this.clock = nanoClock;
    activationEventTranslator =
        (event, sequence, active, previouslyActive) ->
            event.activation(
                active, Thread.currentThread().getId(), previouslyActive, clock.nanoTime(), clock);
    deactivationEventTranslator =
        (event, sequence, active, previouslyActive) ->
            event.deactivation(
                active, Thread.currentThread().getId(), previouslyActive, clock.nanoTime(), clock);
    this.eventBuffer = createRingBuffer();
    this.sequence = new Sequence();
    // tells the ring buffer to not override slots which have not been read yet
    this.eventBuffer.addGatingSequences(sequence);
    this.poller = eventBuffer.newPoller();
    contextForLogging = new TraceContext();
    this.callTreePool =
        ObjectPool.createRecyclable(
            2 * 1024,
            new Allocator<CallTree>() {
              @Override
              public CallTree createInstance() {
                return new CallTree();
              }
            });
    // call tree roots are pooled so that fast activations/deactivations with no associated stack
    // traces don't cause allocations
    this.rootPool =
        ObjectPool.createRecyclable(
            512,
            new Allocator<CallTree.Root>() {
              @Override
              public CallTree.Root createInstance() {
                return new CallTree.Root();
              }
            });
    this.jfrFile = jfrFile;
    activationEventsBuffer = ByteBuffer.allocateDirect(ACTIVATION_EVENTS_BUFFER_SIZE);
    this.activationEventsFile = activationEventsFile;
    profiler = loadProfiler();
    activationListener = ProfilingActivationListener.register(this);
  }

  private AsyncProfiler loadProfiler() {
    String libDir = config.getProfilerLibDirectory();
    try {
      Files.createDirectories(Paths.get(libDir));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create directory to extract lib to", e);
    }
    System.setProperty(LIB_DIR_PROPERTY_NAME, libDir);
    return AsyncProfiler.getInstance();
  }

  /**
   * For testing only! This method must only be called in tests and some period after activation /
   * deactivation events, as otherwise it is racy.
   *
   * @param thread the Thread to check.
   * @return true, if profiling is active for the given thread.
   */
  boolean isProfilingActiveOnThread(Thread thread) {
    return profiledThreads.containsKey(thread.getId());
  }

  private synchronized void createFilesIfRequired() throws IOException {
    if (jfrFile == null || !jfrFile.exists()) {
      jfrFile = File.createTempFile("apm-traces-", ".jfr");
      jfrFile.deleteOnExit();
      canDeleteJfrFile = true;
    }
    if (activationEventsFile == null || !activationEventsFile.exists()) {
      activationEventsFile = File.createTempFile("apm-activation-events-", ".bin");
      activationEventsFile.deleteOnExit();
      canDeleteActivationEventsFile = true;
    }
    if (activationEventsFileChannel == null || !activationEventsFileChannel.isOpen()) {
      activationEventsFileChannel =
          FileChannel.open(
              activationEventsFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
    }
    if (activationEventsFileChannel.size() == 0) {
      preAllocate(activationEventsFileChannel, PRE_ALLOCATE_ACTIVATION_EVENTS_FILE_MB);
    }
  }

  /**
   * Makes sure that the first blocks of the file are contiguous to provide fast sequential access
   */
  private static void preAllocate(FileChannel channel, int mb) throws IOException {
    long initialPos = channel.position();
    ByteBuffer oneKb = ByteBuffer.allocate(1024);
    for (int i = 0; i < mb * 1024; i++) {
      channel.write(oneKb);
      ((Buffer) oneKb).clear();
    }
    channel.position(initialPos);
  }

  private static RingBuffer<ActivationEvent> createRingBuffer() {
    return RingBuffer.<ActivationEvent>createMultiProducer(
        new EventFactory<ActivationEvent>() {
          @Override
          public ActivationEvent newInstance() {
            return new ActivationEvent();
          }
        },
        RING_BUFFER_SIZE,
        new NoWaitStrategy());
  }

  /**
   * Called whenever a span is activated.
   *
   * <p>This and {@link #onDeactivation} are the only methods which are executed in a multi-threaded
   * context.
   *
   * @param activeSpan the span which is about to be activated
   * @param previouslyActive the span which has previously been activated
   * @return {@code true}, if the event could be processed, {@code false} if the internal event
   *     queue is full which means the event has been discarded
   */
  public boolean onActivation(Span activeSpan, @Nullable Span previouslyActive) {
    if (profilingSessionOngoing) {
      if (previouslyActive == null) {
        profiler.addThread(Thread.currentThread());
      }
      boolean success =
          eventBuffer.tryPublishEvent(activationEventTranslator, activeSpan, previouslyActive);
      if (!success) {
        logger.fine("Could not add activation event to ring buffer as no slots are available");
      }
      return success;
    }
    return false;
  }

  /**
   * Called whenever a span is deactivated.
   *
   * <p>This and {@link #onActivation} are the only methods which are executed in a multi-threaded
   * context.
   *
   * @param activeSpan the span which is about to be activated
   * @param previouslyActive the span which has previously been activated
   * @return {@code true}, if the event could be processed, {@code false} if the internal event
   *     queue is full which means the event has been discarded
   */
  public boolean onDeactivation(Span activeSpan, @Nullable Span previouslyActive) {
    if (profilingSessionOngoing) {
      if (previouslyActive == null) {
        profiler.removeThread(Thread.currentThread());
      }
      boolean success =
          eventBuffer.tryPublishEvent(deactivationEventTranslator, activeSpan, previouslyActive);
      if (!success) {
        logger.fine("Could not add deactivation event to ring buffer as no slots are available");
      }
      return success;
    }
    return false;
  }

  @Override
  @SuppressWarnings("FutureReturnValueIgnored")
  public void run() {

    // lazily create temporary files
    try {
      createFilesIfRequired();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "unable to initialize profiling files", e);
      return;
    }

    Duration profilingDuration = config.getProfilingDuration();
    boolean postProcessingEnabled = config.isPostProcessingEnabled();

    setProfilingSessionOngoing(postProcessingEnabled);

    if (postProcessingEnabled) {
      logger.fine("Start full profiling session (async-profiler and agent processing)");
    } else {
      logger.fine("Start async-profiler profiling session");
    }
    try {
      profile(profilingDuration);
    } catch (Throwable t) {
      setProfilingSessionOngoing(false);
      logger.log(Level.SEVERE, "Stopping profiler", t);
      return;
    }
    logger.fine("End profiling session");

    boolean interrupted = Thread.currentThread().isInterrupted();
    boolean continueProfilingSession =
        config.isNonStopProfiling() && !interrupted && postProcessingEnabled;
    setProfilingSessionOngoing(continueProfilingSession);

    if (!interrupted && !scheduler.isShutdown()) {
      long delay = config.getProfilingInterval().toMillis() - profilingDuration.toMillis();
      scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
    }
  }

  @SuppressWarnings({"NonAtomicVolatileUpdate", "EmptyCatch"})
  private void profile(Duration profilingDuration) throws Exception {
    try {
      String startCommand = createStartCommand();
      String startMessage = profiler.execute(startCommand);
      logger.fine(startMessage);
      if (!profiledThreads.isEmpty()) {
        restoreFilterState(profiler);
      }
      // Doesn't need to be atomic as this field is being updated only by a single thread
      profilingSessions++;

      // When post-processing is disabled activation events are ignored, but we still need to invoke
      // this method
      // as it is the one enforcing the sampling session duration. As a side effect it will also
      // consume
      // residual activation events if post-processing is disabled dynamically
      consumeActivationEventsFromRingBufferAndWriteToFile(profilingDuration);

      String stopMessage = profiler.execute("stop");
      logger.fine(stopMessage);

      // When post-processing is disabled, jfr file will not be parsed and the heavy processing will
      // not occur
      // as this method aborts when no activation events are buffered
      processTraces();
    } catch (InterruptedException | ClosedByInterruptException e) {
      try {
        profiler.stop();
      } catch (IllegalStateException ignore) {
      }
      Thread.currentThread().interrupt();
    }
  }

  String createStartCommand() {
    StringBuilder startCommand =
        new StringBuilder("start,jfr,event=wall,cstack=n,interval=")
            .append(config.getSamplingInterval().toMillis())
            .append("ms,filter,file=")
            .append(jfrFile)
            .append(",safemode=")
            .append(config.getAsyncProfilerSafeMode());
    if (!config.isProfilingLoggingEnabled()) {
      startCommand.append(",loglevel=none");
    }
    return startCommand.toString();
  }

  /**
   * When doing continuous profiling (interval=duration), we have to tell async-profiler which
   * threads it should profile after re-starting it.
   */
  private void restoreFilterState(AsyncProfiler asyncProfiler) {
    threadMatcher.forEachThread(
        new ThreadMatcher.NonCapturingPredicate<Thread, Long2ObjectHashMap<?>.KeySet>() {
          @Override
          public boolean test(Thread thread, Long2ObjectHashMap<?>.KeySet profiledThreads) {
            return profiledThreads.contains(thread.getId());
          }
        },
        profiledThreads.keySet(),
        new ThreadMatcher.NonCapturingConsumer<Thread, AsyncProfiler>() {
          @Override
          public void accept(Thread thread, AsyncProfiler asyncProfiler) {
            asyncProfiler.addThread(thread);
          }
        },
        asyncProfiler);
  }

  @SuppressWarnings("NullAway")
  private void consumeActivationEventsFromRingBufferAndWriteToFile(Duration profilingDuration)
      throws Exception {
    resetActivationEventBuffer();
    long threshold = System.currentTimeMillis() + profilingDuration.toMillis();
    long initialSleep = 100_000;
    long maxSleep = 10_000_000;
    long sleep = initialSleep;
    while (System.currentTimeMillis() < threshold && !Thread.currentThread().isInterrupted()) {
      if (activationEventsFileChannel.position() < MAX_ACTIVATION_EVENTS_FILE_SIZE) {
        EventPoller.PollState poll = consumeActivationEventsFromRingBufferAndWriteToFile();
        if (poll == EventPoller.PollState.PROCESSING) {
          sleep = initialSleep;
          // don't sleep, after consuming the events there might be new ones in the ring buffer
        } else {
          if (sleep < maxSleep) {
            sleep *= 2;
          }
          LockSupport.parkNanos(sleep);
        }
      } else {
        logger.warning("The activation events file is full. Try lowering the profiling_duration.");
        // the file is full, sleep the rest of the profilingDuration
        Thread.sleep(Math.max(0, threshold - System.currentTimeMillis()));
      }
    }
  }

  EventPoller.PollState consumeActivationEventsFromRingBufferAndWriteToFile() throws Exception {
    createFilesIfRequired();
    return poller.poll(writeActivationEventToFileHandler);
  }

  public void processTraces() throws IOException {
    if (jfrParser == null) {
      jfrParser = new JfrParser();
    }
    if (Thread.currentThread().isInterrupted()) {
      return;
    }
    createFilesIfRequired();

    long eof = startProcessingActivationEventsFile();
    if (eof == 0 && activationEventsBuffer.limit() == 0 && profiledThreads.isEmpty()) {
      logger.fine("No activation events during this period. Skip processing stack traces.");
      return;
    }
    long start = System.nanoTime();
    List<WildcardMatcher> excludedClasses = config.getExcludedClasses();
    List<WildcardMatcher> includedClasses = config.getIncludedClasses();
    if (config.isBackupDiagnosticFiles()) {
      backupDiagnosticFiles(eof);
    }
    try {
      Objects.requireNonNull(jfrFile);
      jfrParser.parse(jfrFile, excludedClasses, includedClasses);
      List<StackTraceEvent> stackTraceEvents = getSortedStackTraceEvents(jfrParser);
      if (logger.isLoggable(Level.FINE)) {
        logger.log(Level.FINE, "Processing {0} stack traces", stackTraceEvents.size());
      }
      List<StackFrame> stackFrames = new ArrayList<>();
      ActivationEvent event = new ActivationEvent();
      long inferredSpansMinDuration = getInferredSpansMinDurationNs();
      for (StackTraceEvent stackTrace : stackTraceEvents) {
        processActivationEventsUpTo(stackTrace.nanoTime, eof, event);
        CallTree.Root root = profiledThreads.get(stackTrace.threadId);
        if (root != null) {
          jfrParser.resolveStackTrace(stackTrace.stackTraceId, stackFrames, MAX_STACK_DEPTH);
          if (stackFrames.size() == MAX_STACK_DEPTH) {
            logger.fine(
                "Max stack depth reached. Set profiling_included_classes or profiling_excluded_classes.");
          }
          // stack frames may not contain any Java frames
          // see
          // https://github.com/jvm-profiling-tools/async-profiler/issues/271#issuecomment-582430233
          if (!stackFrames.isEmpty()) {
            try {
              root.addStackTrace(
                  stackFrames, stackTrace.nanoTime, callTreePool, inferredSpansMinDuration);
            } catch (Throwable e) {
              logger.log(
                  Level.WARNING,
                  "Removing call tree for thread {0} because of exception while adding a stack trace: {1} {2}",
                  new Object[] {stackTrace.threadId, e.getClass(), e.getMessage()});
              logger.log(Level.FINE, e.getMessage(), e);
              profiledThreads.remove(stackTrace.threadId);
            }
          }
        }
        stackFrames.clear();
      }
      // process all activation events that happened after the last stack trace event
      // otherwise we may miss root deactivations
      processActivationEventsUpTo(System.nanoTime(), eof, event);
    } finally {
      if (logger.isLoggable(Level.FINE)) {
        logger.log(Level.FINE, "Processing traces took {0}us", (System.nanoTime() - start) / 1000);
      }
      jfrParser.resetState();
      resetActivationEventBuffer();
    }
  }

  @SuppressWarnings({"NullAway", "JavaUtilDate"})
  private void backupDiagnosticFiles(long eof) throws IOException {
    String now = String.format("%tFT%<tT.%<tL", new Date());
    Path profilerDir = Paths.get(System.getProperty("java.io.tmpdir"), "profiler");
    profilerDir.toFile().mkdir();

    try (FileChannel activationsFile =
        FileChannel.open(
            profilerDir.resolve(now + "-activations.dat"),
            StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE)) {
      if (eof > 0) {
        activationEventsFileChannel.transferTo(0, eof, activationsFile);
      } else {
        int position = activationEventsBuffer.position();
        activationsFile.write(activationEventsBuffer);
        activationEventsBuffer.position(position);
      }
    }
    Files.copy(jfrFile.toPath(), profilerDir.resolve(now + "-traces.jfr"));
  }

  private long getInferredSpansMinDurationNs() {
    return config.getInferredSpansMinDuration().toNanos();
  }

  /**
   * Returns stack trace events of relevant threads sorted by timestamp. The events in the JFR file
   * are not in order. Even for the same thread, a more recent event might come before an older
   * event. In order to be able to correlate stack trace events and activation events, both need to
   * be in order.
   *
   * <p>Returns only events for threads where at least one activation happened (because only those
   * are profiled by async-profiler)
   */
  private static List<StackTraceEvent> getSortedStackTraceEvents(JfrParser jfrParser) throws IOException {
    List<StackTraceEvent> stackTraceEvents = new ArrayList<>();
    jfrParser.consumeStackTraces(
        new JfrParser.StackTraceConsumer() {
          @Override
          public void onCallTree(long threadId, long stackTraceId, long nanoTime) {
            stackTraceEvents.add(new StackTraceEvent(nanoTime, stackTraceId, threadId));
          }
        });
    Collections.sort(stackTraceEvents);
    return stackTraceEvents;
  }

  void processActivationEventsUpTo(long timestamp, long eof) throws IOException {
    processActivationEventsUpTo(timestamp, eof, new ActivationEvent());
  }
  @SuppressWarnings("NullAway")
  public void processActivationEventsUpTo(long timestamp, long eof, ActivationEvent event)
      throws IOException {
    FileChannel activationEventsFileChannel = this.activationEventsFileChannel;
    ByteBuffer buf = activationEventsBuffer;
    long previousTimestamp = 0;
    while (buf.hasRemaining() || activationEventsFileChannel.position() < eof) {
      if (!buf.hasRemaining()) {
        readActivationEventsToBuffer(activationEventsFileChannel, eof, buf);
      }
      long eventTimestamp = peekLong(buf);
      if (eventTimestamp < previousTimestamp && logger.isLoggable(Level.FINE)) {
        logger.log(
            Level.FINE,
            "Timestamp of current activation event ({0}) is lower than the one from the previous event ({1})",
            new Object[] {eventTimestamp, previousTimestamp});
      }
      previousTimestamp = eventTimestamp;
      if (eventTimestamp <= timestamp) {
        event.deserialize(buf);
        try {
          event.handle(this);
        } catch (Throwable e) {
          logger.log(
              Level.WARNING,
              "Removing call tree for thread {0} because of exception while handling activation event: {1} {2}",
              new Object[] {event.threadId, e.getClass(), e.getMessage()});
          logger.log(Level.FINE, e.getMessage(), e);
          profiledThreads.remove(event.threadId);
        }
      } else {
        return;
      }
    }
  }

  private static void readActivationEventsToBuffer(
      FileChannel activationEventsFileChannel, long eof, ByteBuffer byteBuffer) throws IOException {
    Buffer buf = byteBuffer;
    buf.clear();
    long remaining = eof - activationEventsFileChannel.position();
    activationEventsFileChannel.read(byteBuffer);
    buf.flip();
    if (remaining < buf.capacity()) {
      buf.limit((int) remaining);
    }
  }

  private static long peekLong(ByteBuffer buf) {
    int pos = buf.position();
    try {
      return buf.getLong();
    } finally {
      ((Buffer) buf).position(pos);
    }
  }

  public void resetActivationEventBuffer() throws IOException {
    ((Buffer) activationEventsBuffer).clear();
    if (activationEventsFileChannel != null && activationEventsFileChannel.isOpen()) {
      activationEventsFileChannel.position(0L);
    }
  }

  @SuppressWarnings("NullAway")
  private void flushActivationEvents() throws IOException {
    if (activationEventsBuffer.position() > 0) {
      ((Buffer) activationEventsBuffer).flip();
      activationEventsFileChannel.write(activationEventsBuffer);
      ((Buffer) activationEventsBuffer).clear();
    }
  }

  @SuppressWarnings("NullAway")
  long startProcessingActivationEventsFile() throws IOException {
    Buffer activationEventsBuffer = this.activationEventsBuffer;
    if (activationEventsFileChannel.position() > 0) {
      flushActivationEvents();
      activationEventsBuffer.limit(0);
    } else {
      activationEventsBuffer.flip();
    }
    long eof = activationEventsFileChannel.position();
    activationEventsFileChannel.position(0);
    return eof;
  }

  @SuppressWarnings("NullAway")
  void copyFromFiles(Path activationEvents, Path traces) throws IOException {
    createFilesIfRequired();

    FileChannel otherActivationsChannel = FileChannel.open(activationEvents, READ);
    activationEventsFileChannel.transferFrom(
        otherActivationsChannel, 0, otherActivationsChannel.size());
    activationEventsFileChannel.position(otherActivationsChannel.size());
    FileChannel otherTracesChannel = FileChannel.open(traces, READ);
    FileChannel.open(jfrFile.toPath(), WRITE)
        .transferFrom(otherTracesChannel, 0, otherTracesChannel.size());
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public void start() {
    scheduler.submit(this);
  }

  public void stop() throws InterruptedException, IOException {
    // cancels/interrupts the profiling thread
    // implicitly clears profiled threads
    scheduler.shutdown();
    scheduler.awaitTermination(10, TimeUnit.SECONDS);

    activationListener.close();

    if (activationEventsFileChannel != null) {
      activationEventsFileChannel.close();
    }

    if (jfrFile != null && canDeleteJfrFile) {
      jfrFile.delete();
    }
    if (activationEventsFile != null && canDeleteActivationEventsFile) {
      activationEventsFile.delete();
    }
  }

  void setProfilingSessionOngoing(boolean profilingSessionOngoing) {
    this.profilingSessionOngoing = profilingSessionOngoing;
    if (!profilingSessionOngoing) {
      clearProfiledThreads();
    } else if (!profiledThreads.isEmpty() && logger.isLoggable(Level.FINE)) {
      logger.log(Level.FINE, "Retaining {0} call tree roots", profiledThreads.size());
    }
  }

  public void clearProfiledThreads() {
    for (CallTree.Root root : profiledThreads.values()) {
      root.recycle(callTreePool, rootPool);
    }
    profiledThreads.clear();
  }

  // for testing
  CallTree.Root getRoot() {
    return profiledThreads.get(Thread.currentThread().getId());
  }

  int getProfilingSessions() {
    return profilingSessions;
  }

  public SpanAnchoredClock getClock() {
    return clock;
  }

  public static class StackTraceEvent implements Comparable<StackTraceEvent> {
    private final long nanoTime;
    private final long stackTraceId;
    private final long threadId;

    private StackTraceEvent(long nanoTime, long stackTraceId, long threadId) {
      this.nanoTime = nanoTime;
      this.stackTraceId = stackTraceId;
      this.threadId = threadId;
    }

    public long getThreadId() {
      return threadId;
    }

    public long getNanoTime() {
      return nanoTime;
    }

    public long getStackTraceId() {
      return stackTraceId;
    }

    @Override
    public int compareTo(StackTraceEvent o) {
      return Long.compare(nanoTime, o.nanoTime);
    }
  }

  private static class ActivationEvent {
    public static final int SERIALIZED_SIZE =
        Long.SIZE / Byte.SIZE
            + // timestamp
            TraceContext.SERIALIZED_LENGTH
            + // traceContextBuffer
            TraceContext.SERIALIZED_LENGTH
            + // previousContextBuffer
            1
            + // rootContext
            Long.SIZE / Byte.SIZE
            + // threadId
            1; // activation

    private long timestamp;
    private final byte[] traceContextBuffer = new byte[TraceContext.SERIALIZED_LENGTH];
    private final byte[] previousContextBuffer = new byte[TraceContext.SERIALIZED_LENGTH];
    private boolean rootContext;
    private long threadId;
    private boolean activation;

    public void activation(
        Span context,
        long threadId,
        @Nullable Span previousContext,
        long nanoTime,
        SpanAnchoredClock clock) {
      set(context, threadId, /*activation=*/ true, previousContext, nanoTime, clock);
    }

    public void deactivation(
        Span context,
        long threadId,
        @Nullable Span previousContext,
        long nanoTime,
        SpanAnchoredClock clock) {
      set(context, threadId, /*activation=*/ false, previousContext, nanoTime, clock);
    }

    private void set(
        Span traceContext,
        long threadId,
        boolean activation,
        @Nullable Span previousContext,
        long nanoTime,
        SpanAnchoredClock clock) {
      TraceContext.serialize(traceContext, clock.getAnchor(traceContext), traceContextBuffer);
      this.threadId = threadId;
      this.activation = activation;
      if (previousContext != null) {
        TraceContext.serialize(
            previousContext, clock.getAnchor(previousContext), previousContextBuffer);
        rootContext = false;
      } else {
        rootContext = true;
      }
      this.timestamp = nanoTime;
    }

    public void handle(SamplingProfiler samplingProfiler) {
      if (logger.isLoggable(Level.FINE)) {
        logger.log(
            Level.FINE,
            "Handling event timestamp={0} root={1} threadId={2} activation={3}",
            new Object[] {timestamp, rootContext, threadId, activation});
      }
      if (activation) {
        handleActivationEvent(samplingProfiler);
      } else {
        handleDeactivationEvent(samplingProfiler);
      }
    }

    private void handleActivationEvent(SamplingProfiler samplingProfiler) {
      if (rootContext) {
        startProfiling(samplingProfiler);
      } else {
        CallTree.Root root = samplingProfiler.profiledThreads.get(threadId);
        if (root != null) {
          if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Handling activation for thread {0}", threadId);
          }
          root.onActivation(traceContextBuffer, timestamp);
        } else if (logger.isLoggable(Level.FINE)) {
          logger.log(
              Level.FINE,
              "Illegal state when handling activation event for thread {0}: no root found for this thread",
              threadId);
        }
      }
    }

    private void startProfiling(SamplingProfiler samplingProfiler) {
      CallTree.Root root =
          CallTree.createRoot(samplingProfiler.rootPool, traceContextBuffer, timestamp);
      if (logger.isLoggable(Level.FINE)) {
        logger.log(
            Level.FINE,
            "Create call tree ({0}) for thread {1}",
            new Object[] {deserialize(samplingProfiler, traceContextBuffer), threadId});
      }

      CallTree.Root orphaned = samplingProfiler.profiledThreads.put(threadId, root);
      if (orphaned != null) {
        if (logger.isLoggable(Level.FINE)) {
          logger.log(
              Level.FINE,
              "Illegal state when stopping profiling for thread {0}: orphaned root",
              threadId);
        }
        orphaned.recycle(samplingProfiler.callTreePool, samplingProfiler.rootPool);
      }
    }

    private void handleDeactivationEvent(SamplingProfiler samplingProfiler) {
      if (rootContext) {
        stopProfiling(samplingProfiler);
      } else {
        CallTree.Root root = samplingProfiler.profiledThreads.get(threadId);
        if (root != null) {
          if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Handling deactivation for thread {0}", threadId);
          }
          root.onDeactivation(traceContextBuffer, previousContextBuffer, timestamp);
        } else if (logger.isLoggable(Level.FINE)) {
          logger.log(
              Level.FINE,
              "Illegal state when handling deactivation event for thread {0}: no root found for this thread",
              threadId);
        }
      }
    }

    private void stopProfiling(SamplingProfiler samplingProfiler) {
      CallTree.Root callTree = samplingProfiler.profiledThreads.get(threadId);
      if (callTree != null && callTree.getRootContext().traceIdAndIdEquals(traceContextBuffer)) {
        if (logger.isLoggable(Level.FINE)) {
          logger.log(
              Level.FINE,
              "End call tree ({0}) for thread {1}",
              new Object[] {deserialize(samplingProfiler, traceContextBuffer), threadId});
        }
        samplingProfiler.profiledThreads.remove(threadId);
        try {
          callTree.end(
              samplingProfiler.callTreePool, samplingProfiler.getInferredSpansMinDurationNs());
          int createdSpans =
              callTree.spanify(samplingProfiler.getClock(), samplingProfiler.tracerProvider.get());
          if (logger.isLoggable(Level.FINE)) {
            if (createdSpans > 0) {
              logger.log(
                  Level.FINE,
                  "Created spans ({0}) for thread {1}",
                  new Object[] {createdSpans, threadId});
            } else {
              logger.log(
                  Level.FINE,
                  "Created no spans for thread {0} (count={1})",
                  new Object[] {threadId, callTree.getCount()});
            }
          }
        } finally {
          callTree.recycle(samplingProfiler.callTreePool, samplingProfiler.rootPool);
        }
      }
    }

    public void serialize(ByteBuffer buf) {
      buf.putLong(timestamp);
      buf.put(traceContextBuffer);
      buf.put(previousContextBuffer);
      buf.put(rootContext ? (byte) 1 : (byte) 0);
      buf.putLong(threadId);
      buf.put(activation ? (byte) 1 : (byte) 0);
    }

    public void deserialize(ByteBuffer buf) {
      timestamp = buf.getLong();
      buf.get(traceContextBuffer);
      buf.get(previousContextBuffer);
      rootContext = buf.get() == 1;
      threadId = buf.getLong();
      activation = buf.get() == 1;
    }

    private static TraceContext deserialize(SamplingProfiler samplingProfiler, byte[] traceContextBuffer) {
      samplingProfiler.contextForLogging.deserialize(traceContextBuffer);
      return samplingProfiler.contextForLogging;
    }

  }

  /**
   * Does not wait but immediately returns the highest sequence which is available for read We never
   * want to wait until new elements are available, we just want to process all available events
   */
  private static class NoWaitStrategy implements WaitStrategy {

    @Override
    public long waitFor(
        long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) {
      return dependentSequence.get();
    }

    @Override
    public void signalAllWhenBlocking() {}
  }

  // extracting to a class instead of instantiating an anonymous inner class makes a huge difference
  // in allocations
  private class WriteActivationEventToFileHandler implements EventPoller.Handler<ActivationEvent> {
    @Override
    @SuppressWarnings("NullAway")
    public boolean onEvent(ActivationEvent event, long sequence, boolean endOfBatch)
        throws IOException {
      if (endOfBatch) {
        SamplingProfiler.this.sequence.set(sequence);
      }
      if (activationEventsFileChannel.size() < MAX_ACTIVATION_EVENTS_FILE_SIZE) {
        event.serialize(activationEventsBuffer);
        if (!activationEventsBuffer.hasRemaining()) {
          flushActivationEvents();
        }
        return true;
      }
      return false;
    }
  }
}
