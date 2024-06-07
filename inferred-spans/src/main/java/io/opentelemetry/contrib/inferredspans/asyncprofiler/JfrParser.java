/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.inferredspans.asyncprofiler;

import io.opentelemetry.contrib.inferredspans.StackFrame;
import io.opentelemetry.contrib.inferredspans.config.WildcardMatcher;
import io.opentelemetry.contrib.inferredspans.pooling.Recyclable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.agrona.collections.Int2IntHashMap;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2ObjectHashMap;

/**
 * Parses the binary JFR file created by async-profiler. May not work with JFR files created by an
 * actual flight recorder.
 *
 * <p>The implementation is tuned with to minimize allocations when parsing a JFR file. Most data
 * structures can be reused by first {@linkplain #resetState() resetting the state} and then
 * {@linkplain #parse(File, List, List) parsing} another file.
 */
public class JfrParser implements Recyclable {

  private static final Logger logger = Logger.getLogger(JfrParser.class.getName());

  private static final byte[] MAGIC_BYTES = new byte[] {'F', 'L', 'R', '\0'};
  private static final Set<String> JAVA_FRAME_TYPES =
      new HashSet<>(Arrays.asList("Interpreted", "JIT compiled", "Inlined"));
  private static final int BIG_FILE_BUFFER_SIZE = 5 * 1024 * 1024;
  private static final int SMALL_FILE_BUFFER_SIZE = 4 * 1024;
  private static final String SYMBOL_EXCLUDED = "3x cluded";
  private static final String SYMBOL_NULL = "n u11";
  private static final StackFrame FRAME_EXCLUDED = new StackFrame("excluded", "excluded");
  private static final StackFrame FRAME_NULL = new StackFrame("null", "null");

  private final BufferedFile bufferedFile;
  private final Int2IntHashMap classIdToClassNameSymbolId = new Int2IntHashMap(-1);
  private final Int2IntHashMap symbolIdToPos = new Int2IntHashMap(-1);
  private final Int2ObjectHashMap<String> symbolIdToString = new Int2ObjectHashMap<String>();
  private final Int2IntHashMap stackTraceIdToFilePositions = new Int2IntHashMap(-1);
  private final Long2LongHashMap nativeTidToJavaTid = new Long2LongHashMap(-1);
  private final Long2ObjectHashMap<StackFrame> methodIdToFrame =
      new Long2ObjectHashMap<StackFrame>();
  private final Long2LongHashMap methodIdToMethodNameSymbol = new Long2LongHashMap(-1);
  private final Long2LongHashMap methodIdToClassId = new Long2LongHashMap(-1);
  // used to resolve a symbol with minimal allocations
  private final StringBuilder symbolBuilder = new StringBuilder();
  private long eventsFilePosition;
  private long metadataFilePosition;
  @Nullable private boolean[] isJavaFrameType;
  @Nullable private List<WildcardMatcher> excludedClasses;
  @Nullable private List<WildcardMatcher> includedClasses;

  public JfrParser() {
    this(
        ByteBuffer.allocateDirect(BIG_FILE_BUFFER_SIZE),
        ByteBuffer.allocateDirect(SMALL_FILE_BUFFER_SIZE));
  }

  JfrParser(ByteBuffer bigBuffer, ByteBuffer smallBuffer) {
    bufferedFile = new BufferedFile(bigBuffer, smallBuffer);
  }

  /**
   * Initializes the parser to make it ready for {@link #resolveStackTrace(long, List, int)} to be
   * called.
   *
   * @param file the JFR file to parse
   * @param excludedClasses Class names to exclude in stack traces (has an effect on {@link
   *     #resolveStackTrace(long, List, int)})
   * @param includedClasses Class names to include in stack traces (has an effect on {@link
   *     #resolveStackTrace(long, List, int)})
   * @throws IOException if some I/O error occurs
   */
  public void parse(
      File file, List<WildcardMatcher> excludedClasses, List<WildcardMatcher> includedClasses)
      throws IOException {
    this.excludedClasses = excludedClasses;
    this.includedClasses = includedClasses;
    bufferedFile.setFile(file);
    long fileSize = bufferedFile.size();

    int chunkSize = readChunk(0);
    if (chunkSize < fileSize) {
      throw new IllegalStateException(
          "This implementation does not support reading JFR files containing multiple chunks");
    }
  }

  private int readChunk(int position) throws IOException {
    bufferedFile.position(position);
    if (logger.isLoggable(Level.FINE)) {
      logger.log(Level.FINE, "Parsing JFR chunk at offset", new Object[] {position});
    }
    for (byte magicByte : MAGIC_BYTES) {
      if (bufferedFile.get() != magicByte) {
        throw new IllegalArgumentException("Not a JFR file");
      }
    }
    short major = bufferedFile.getShort();
    short minor = bufferedFile.getShort();
    if (major != 2 || minor != 0) {
      throw new IllegalArgumentException(
          String.format("Can only parse version 2.0. Was %d.%d", major, minor));
    }
    long chunkSize = bufferedFile.getLong();
    long constantPoolOffset = bufferedFile.getLong();
    metadataFilePosition = position + bufferedFile.getLong();
    bufferedFile.getLong(); // startTimeNanos
    bufferedFile.getLong(); // durationNanos
    bufferedFile.getLong(); // startTicks
    bufferedFile.getLong(); // ticksPerSecond
    bufferedFile.getInt(); // features

    // Events start right after metadata
    eventsFilePosition = metadataFilePosition + parseMetadata(metadataFilePosition);
    parseCheckpointEvents(position + constantPoolOffset);
    return (int) chunkSize;
  }

  private long parseMetadata(long metadataOffset) throws IOException {
    bufferedFile.position(metadataOffset);
    int size = bufferedFile.getVarInt();
    expectEventType(EventTypeId.EVENT_METADATA);
    return size;
  }

  private void expectEventType(int expectedEventType) throws IOException {
    long eventType = bufferedFile.getVarLong();
    if (eventType != expectedEventType) {
      throw new IOException("Expected " + expectedEventType + " but got " + eventType);
    }
  }

  private void parseCheckpointEvents(long checkpointOffset) throws IOException {
    bufferedFile.position(checkpointOffset);
    bufferedFile.getVarInt(); // size
    expectEventType(EventTypeId.EVENT_CHECKPOINT);
    bufferedFile.getVarLong(); // start
    bufferedFile.getVarLong(); // duration
    long delta = bufferedFile.getVarLong();
    if (delta != 0) {
      throw new IllegalStateException(
          "Expected only one checkpoint event, but file contained multiple, delta is " + delta);
    }
    bufferedFile.get(); // typeMask
    long poolCount = bufferedFile.getVarLong();
    for (int i = 0; i < poolCount; i++) {
      parseConstantPool();
    }
  }

  private void parseConstantPool() throws IOException {
    long typeId = bufferedFile.getVarLong();
    int count = bufferedFile.getVarInt();

    switch ((int) typeId) {
      case ContentTypeId.CONTENT_FRAME_TYPE:
        readFrameTypeConstants(count);
        break;
      case ContentTypeId.CONTENT_THREAD_STATE:
      case ContentTypeId.CONTENT_GC_WHEN:
      case ContentTypeId.CONTENT_LOG_LEVELS:
        // We are not interested in those types, but still have to consume the bytes
        for (int i = 0; i < count; i++) {
          bufferedFile.getVarInt();
          bufferedFile.skipString();
        }
        break;
      case ContentTypeId.CONTENT_THREAD:
        readThreadConstants(count);
        break;
      case ContentTypeId.CONTENT_STACKTRACE:
        readStackTraceConstants(count);
        break;
      case ContentTypeId.CONTENT_METHOD:
        readMethodConstants(count);
        break;
      case ContentTypeId.CONTENT_CLASS:
        readClassConstants(count);
        break;
      case ContentTypeId.CONTENT_PACKAGE:
        readPackageConstants(count);
        break;
      case ContentTypeId.CONTENT_SYMBOL:
        readSymbolConstants(count);
        break;
      default:
        throw new IllegalStateException("Unhandled constant pool type: " + typeId);
    }
  }

  private void readSymbolConstants(int count) throws IOException {
    for (int i = 0; i < count; i++) {
      int symbolId = bufferedFile.getVarInt();
      int pos = (int) bufferedFile.position();
      bufferedFile.skipString();
      symbolIdToPos.put(symbolId, pos);
      symbolIdToString.put(symbolId, SYMBOL_NULL);
    }
  }

  private void readClassConstants(int count) throws IOException {
    for (int i = 0; i < count; i++) {
      int classId = bufferedFile.getVarInt();
      bufferedFile.getVarInt(); // classloader, always zero in async-profiler JFR files
      int classNameSymbolId = bufferedFile.getVarInt();
      classIdToClassNameSymbolId.put(classId, classNameSymbolId); // class name
      bufferedFile.getVarInt(); // package symbol id
      bufferedFile.getVarInt(); // access flags
    }
  }

  private void readMethodConstants(int count) throws IOException {
    for (int i = 0; i < count; i++) {
      long id = bufferedFile.getVarLong();
      int classId = bufferedFile.getVarInt();
      // symbol ids are incrementing integers, no way there are more than 2 billion distinct
      // ones
      int methodNameSymbolId = bufferedFile.getVarInt();
      methodIdToFrame.put(id, FRAME_NULL);
      methodIdToClassId.put(id, classId);
      methodIdToMethodNameSymbol.put(id, methodNameSymbolId);
      bufferedFile.getVarLong(); // signature
      bufferedFile.getVarInt(); // modifiers
      bufferedFile.get(); // hidden
    }
  }

  private void readPackageConstants(int count) throws IOException {
    for (int i = 0; i < count; i++) {
      bufferedFile.getVarLong(); // id
      bufferedFile.getVarLong(); // symbol-id of package name
    }
  }

  private void readThreadConstants(int count) throws IOException {
    for (int i = 0; i < count; i++) {
      int nativeThreadId = bufferedFile.getVarInt();
      bufferedFile.skipString(); // native thread name
      bufferedFile.getVarInt(); // native thread ID again
      bufferedFile.skipString(); // java thread name
      long javaThreadId = bufferedFile.getVarLong();
      if (javaThreadId != 0) { // javaThreadId will be null for native-only threads
        nativeTidToJavaTid.put(nativeThreadId, javaThreadId);
      }
    }
  }

  private void readStackTraceConstants(int count) throws IOException {
    for (int i = 0; i < count; i++) {

      int stackTraceId = bufferedFile.getVarInt();
      bufferedFile.get(); // truncated byte, always zero anyway

      this.stackTraceIdToFilePositions.put(stackTraceId, (int) bufferedFile.position());
      // We need to skip the stacktrace to get to the position of the next one
      readOrSkipStacktraceFrames(null, 0);
    }
  }

  private void readFrameTypeConstants(int count) throws IOException {
    isJavaFrameType = new boolean[count];
    for (int i = 0; i < count; i++) {
      int id = bufferedFile.getVarInt();
      if (i != id) {
        throw new IllegalStateException("Expecting ids to be incrementing");
      }
      isJavaFrameType[id] = JAVA_FRAME_TYPES.contains(bufferedFile.readString());
    }
  }

  /**
   * Invokes the callback for each stack trace event in the JFR file.
   *
   * @param callback called for each stack trace event
   * @throws IOException if some I/O error occurs
   */
  public void consumeStackTraces(StackTraceConsumer callback) throws IOException {
    if (!bufferedFile.isSet()) {
      throw new IllegalStateException("consumeStackTraces was called before parse");
    }
    bufferedFile.position(eventsFilePosition);
    long fileSize = bufferedFile.size();
    long eventStart = eventsFilePosition;
    while (eventStart < fileSize) {
      bufferedFile.position(eventStart);
      int eventSize = bufferedFile.getVarInt();
      long eventType = bufferedFile.getVarLong();
      if (eventType == EventTypeId.EVENT_EXECUTION_SAMPLE) {
        long nanoTime = bufferedFile.getVarLong();
        int tid = bufferedFile.getVarInt();
        int stackTraceId = bufferedFile.getVarInt();
        bufferedFile.getVarInt(); // thread state
        long javaThreadId = nativeTidToJavaTid.get(tid);
        callback.onCallTree(javaThreadId, stackTraceId, nanoTime);
      }
      eventStart += eventSize;
    }
  }

  /**
   * Resolves the stack trace with the given {@code stackTraceId}. Only java frames will be
   * included.
   *
   * <p>Note that his allocates strings for symbols in case a stack frame has not already been
   * resolved for the current JFR file yet. These strings are currently not cached so this can
   * create some GC pressure.
   *
   * <p>Excludes frames based on the {@link WildcardMatcher}s supplied to {@link #parse(File, List,
   * List)}.
   *
   * @param stackTraceId The id of the stack traced. Used to look up the position of the file in
   *     which the given stack trace is stored via {@link #stackTraceIdToFilePositions}.
   * @param stackFrames The mutable list where the stack frames are written to. Don't forget to
   *     {@link List#clear()} the list before calling this method if the list is reused.
   * @param maxStackDepth The max size of the stackFrames list (excluded frames don't take up
   *     space). In contrast to async-profiler's {@code jstackdepth} argument this does not truncate
   *     the bottom of the stack, only the top. This is important to properly create a call tree
   *     without making it overly complex.
   * @throws IOException if there is an error reading in current buffer
   */
  public void resolveStackTrace(long stackTraceId, List<StackFrame> stackFrames, int maxStackDepth)
      throws IOException {
    if (!bufferedFile.isSet()) {
      throw new IllegalStateException("getStackTrace was called before parse");
    }
    bufferedFile.position(stackTraceIdToFilePositions.get((int) stackTraceId));
    readOrSkipStacktraceFrames(stackFrames, maxStackDepth);
  }

  private void readOrSkipStacktraceFrames(@Nullable List<StackFrame> stackFrames, int maxStackDepth)
      throws IOException {
    int frameCount = bufferedFile.getVarInt();
    for (int i = 0; i < frameCount; i++) {
      int methodId = bufferedFile.getVarInt();
      bufferedFile.getVarInt(); // line number
      bufferedFile.getVarInt(); // bytecode index
      byte type = bufferedFile.get();
      if (stackFrames != null) {
        addFrameIfIncluded(stackFrames, methodId, type);
        if (stackFrames.size() > maxStackDepth) {
          stackFrames.remove(0);
        }
      }
    }
  }

  @SuppressWarnings("ReferenceEquality")
  private void addFrameIfIncluded(List<StackFrame> stackFrames, int methodId, byte frameType)
      throws IOException {
    if (isJavaFrameType(frameType)) {
      StackFrame stackFrame = resolveStackFrame(methodId);
      if (stackFrame != FRAME_EXCLUDED) {
        stackFrames.add(stackFrame);
      }
    }
  }

  private boolean isJavaFrameType(byte frameType) {
    assert isJavaFrameType != null;
    return isJavaFrameType[frameType];
  }

  @SuppressWarnings("ReferenceEquality")
  private String resolveSymbol(int id, boolean classSymbol) throws IOException {
    String symbol = symbolIdToString.get(id);
    if (symbol != SYMBOL_NULL) {
      return symbol;
    }

    long previousPosition = bufferedFile.position();
    int position = symbolIdToPos.get(id);
    bufferedFile.position(position);
    symbolBuilder.setLength(0);
    bufferedFile.readString(symbolBuilder);
    bufferedFile.position(previousPosition);

    if (classSymbol) {
      replaceSlashesWithDots(symbolBuilder);
    }

    if (classSymbol && !isClassIncluded(symbolBuilder)) {
      symbol = SYMBOL_EXCLUDED;
    } else {
      symbol = symbolBuilder.toString();
    }
    symbolIdToString.put(id, symbol);
    return symbol;
  }

  private static void replaceSlashesWithDots(StringBuilder builder) {
    for (int i = 0; i < builder.length(); i++) {
      if (builder.charAt(i) == '/') {
        builder.setCharAt(i, '.');
      }
    }
  }

  private boolean isClassIncluded(CharSequence className) {
    assert includedClasses != null;
    assert excludedClasses != null;
    return WildcardMatcher.isAnyMatch(includedClasses, className)
        && WildcardMatcher.isNoneMatch(excludedClasses, className);
  }

  @SuppressWarnings("ReferenceEquality")
  private StackFrame resolveStackFrame(long frameId) throws IOException {
    StackFrame stackFrame = methodIdToFrame.get(frameId);
    if (stackFrame != FRAME_NULL) {
      return stackFrame;
    }
    String className =
        resolveSymbol(
            classIdToClassNameSymbolId.get((int) methodIdToClassId.get(frameId)),
            /* classSymbol= */ true);
    if (className == SYMBOL_EXCLUDED) {
      stackFrame = FRAME_EXCLUDED;
    } else {
      String method =
          resolveSymbol((int) methodIdToMethodNameSymbol.get(frameId), /* classSymbol= */ false);
      stackFrame = new StackFrame(className, Objects.requireNonNull(method));
    }
    methodIdToFrame.put(frameId, stackFrame);
    return stackFrame;
  }

  @Override
  public void resetState() {
    bufferedFile.resetState();
    eventsFilePosition = 0;
    metadataFilePosition = 0;
    isJavaFrameType = null;
    classIdToClassNameSymbolId.clear();
    stackTraceIdToFilePositions.clear();
    methodIdToFrame.clear();
    methodIdToMethodNameSymbol.clear();
    methodIdToClassId.clear();
    symbolBuilder.setLength(0);
    excludedClasses = null;
    includedClasses = null;
    symbolIdToPos.clear();
    symbolIdToString.clear();
  }

  public interface StackTraceConsumer {

    /**
     * Callback invoked from {@link JfrParser} when a execution sample is encountered.
     *
     * @param threadId The {@linkplain Thread#getId() Java thread id} for with the event was
     *     recorded.
     * @param stackTraceId The id of the stack trace event. Can be used to resolve the stack trace
     *     via {@link #resolveStackTrace(long, List, int)}
     * @param nanoTime The timestamp of the event which can be correlated with {@link
     *     System#nanoTime()}
     * @throws IOException if there is any error reading stack trace
     */
    void onCallTree(long threadId, long stackTraceId, long nanoTime) throws IOException;
  }

  private static class EventTypeId {

    private EventTypeId() {}

    static final int EVENT_METADATA = 0;
    static final int EVENT_CHECKPOINT = 1;

    // The following event types actually are defined in the metadata of the JFR file itself
    // for simplicity and performance, we hardcode the values used by the async-profiler
    // implementation
    static final int EVENT_EXECUTION_SAMPLE = 101;
  }

  private static final class ContentTypeId {
    private ContentTypeId() {}

    static final int CONTENT_THREAD = 22;
    static final int CONTENT_LOG_LEVELS = 33;
    static final int CONTENT_STACKTRACE = 26;
    static final int CONTENT_CLASS = 21;
    static final int CONTENT_METHOD = 28;
    static final int CONTENT_SYMBOL = 31;
    static final int CONTENT_THREAD_STATE = 25;
    static final int CONTENT_FRAME_TYPE = 24;
    static final int CONTENT_GC_WHEN = 32;
    static final int CONTENT_PACKAGE = 30;
  }
}
