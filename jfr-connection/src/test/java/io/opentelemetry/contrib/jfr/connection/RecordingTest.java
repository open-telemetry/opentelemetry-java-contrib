/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.jfr.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.errorprone.annotations.Keep;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

class RecordingTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordingTest.class);

  static FlightRecorderConnection getFlightRecorderConnection() {
    MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      return FlightRecorderConnection.connect(mBeanServer);
    } catch (IOException e) {
      // possible that this can be thrown, but should not happen in this context
      fail("IOException not expected", e);
    } catch (JfrConnectionException reallyBad) {
      if (reallyBad.getCause() instanceof InstanceNotFoundException) {
        fail(
            "Either JVM does not support JFR, or experimental options need to be enabled",
            reallyBad.getCause());
      } else {
        fail("something really bad happened", reallyBad);
      }
    }
    return null;
  }

  static void deleteJfrFiles() {
    try (Stream<Path> files = Files.list(Paths.get(System.getProperty("user.dir")))) {
      files
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".jfr"))
          .forEach(
              jfrFile -> {
                try {
                  Files.delete(jfrFile);
                } catch (IOException ioException) {
                  LOGGER.info(ioException::getMessage);
                }
              });
    } catch (IOException ioException) {
      LOGGER.info(ioException::getMessage);
    }
  }

  FlightRecorderConnection flightRecorderConnection = null;

  @BeforeEach
  void setup() {
    deleteJfrFiles();
    flightRecorderConnection = getFlightRecorderConnection();
  }

  @AfterEach
  void tearDown() {
    deleteJfrFiles();
  }

  @Test
  void assertNewRecordingInitialValues() {
    try (Recording recording = flightRecorderConnection.newRecording(null, null)) {
      assertEquals(Recording.State.NEW, recording.getState());
      assertEquals(-1, recording.getId());
    } catch (IOException | IllegalStateException | JfrConnectionException exception) {
      fail("assertNewRecordingInitialValues caught exception", exception);
    }
  }

  @Test
  void assertRecordingStartIdAndState() {
    try (Recording recording = flightRecorderConnection.newRecording(null, null)) {
      long id = recording.start();
      assertEquals(id, recording.getId());
      assertEquals(Recording.State.RECORDING, recording.getState());
    } catch (IOException | IllegalStateException | JfrConnectionException e) {
      fail("assertRecordingStartIdAndState caught exception", e);
    }
  }

  @Test
  void assertRecordingStopState() {
    try (Recording recording = flightRecorderConnection.newRecording(null, null)) {
      long id = recording.start();
      assertEquals(id, recording.getId());
      recording.stop();
      assertEquals(Recording.State.STOPPED, recording.getState());
    } catch (IOException | IllegalStateException | JfrConnectionException e) {
      fail("assertRecordingStopState caught exception", e);
    }
  }

  @Test
  void assertRecordingCloseState() {
    try (Recording recording = flightRecorderConnection.newRecording(null, null)) {
      long id = recording.start();
      assertEquals(id, recording.getId());
      recording.close();
      assertEquals(Recording.State.CLOSED, recording.getState());
    } catch (IOException | IllegalStateException | JfrConnectionException e) {
      fail("assertRecordingCloseState caught exception", e);
    }
  }

  @SuppressWarnings("unchecked")
  static void reflectivelyInvokeMethods(Recording recording, Object[] args) throws Exception {
    Class<Recording> clazz = (Class<Recording>) recording.getClass();
    Method[] methods = clazz.getDeclaredMethods();
    for (int argc = 0; argc < args.length; ) {
      String methodName = (String) args[argc++];
      Method method = null;
      for (Method m : methods) {
        if (m.getName().equals(methodName)) {
          if ("getStream".equals(m.getName())) {
            if (m.getParameterTypes().length < 3) {
              // Always pick getStream(Instant,Instant,long)
              continue;
            }
          }
          method = m;
          break;
        }
      }
      if (method == null) {
        throw new NoSuchMethodException(
            methodName + " not found in declared methods of " + clazz.getName());
      }
      Class<?>[] parameterTypes = method.getParameterTypes();
      Object[] methodArgs = new Object[parameterTypes.length];
      int index = 0;
      for (Class<?> type : parameterTypes) {
        if ("boolean".equals(type.getName())) {
          methodArgs[index++] = args[argc++];
        } else if ("long".equals(type.getName())) {
          methodArgs[index++] = args[argc++];
        } else {
          methodArgs[index++] = type.cast(args[argc++]);
        }
      }
      method.invoke(recording, methodArgs);
    }
  }

  // This ArgumentsAggregator aggregates the Arguments.of(blah) into an Object[] arg.
  // Use with @AggregateWith
  static class VarArgsAggregator implements ArgumentsAggregator {
    @Override
    public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) {
      return accessor.toArray();
    }
  }

  @Keep
  private static Stream<Arguments> assertValidStateChangeNoException() {
    return Stream.of(
        Arguments.of("start"),
        Arguments.of("start", "start"),
        Arguments.of("stop"),
        Arguments.of("stop", "stop"),
        Arguments.of("close"),
        Arguments.of("close", "close"),
        Arguments.of("start", "stop"),
        Arguments.of("start", "stop", "start"),
        Arguments.of("start", "stop", "start", "close"),
        Arguments.of("start", "close"),
        Arguments.of("start", "stop", "close"),
        Arguments.of("start", "clone", false),
        Arguments.of("start", "stop", "clone", false),
        Arguments.of("start", "stop", "getStream", null, null, 500000L),
        Arguments.of("start", "dump", "test.jfr", "stop"),
        Arguments.of("start", "stop", "dump", "test.jfr", "stop"),
        Arguments.of("start", "stop", "dump", "test.jfr", "close"));
  }

  @ParameterizedTest
  @MethodSource
  void assertValidStateChangeNoException(@AggregateWith(VarArgsAggregator.class) Object... states) {
    try (Recording recording = flightRecorderConnection.newRecording(null, null)) {
      reflectivelyInvokeMethods(recording, states);
    } catch (Exception e) {
      if (e instanceof InvocationTargetException && e.getCause() instanceof IllegalStateException) {
        fail("IllegalStateException was not expected", e);
      }
      fail("Bad test code", e);
    }
  }

  @Keep
  private static Stream<Arguments> assertInvalidStateChangeThrowsIllegalStateException() {
    return Stream.of(
        Arguments.of("getStream", null, null, 500000L),
        Arguments.of("dump", "test.jfr"),
        Arguments.of("close", "start"),
        Arguments.of("close", "stop"),
        Arguments.of("start", "close", "stop"),
        Arguments.of("start", "close", "clone", false),
        Arguments.of("start", "close", "dump", "test.jfr"),
        Arguments.of("start", "getStream", null, null, 500000L),
        Arguments.of("start", "close", "getStream", null, null, 500000L));
  }

  @ParameterizedTest
  @MethodSource
  void assertInvalidStateChangeThrowsIllegalStateException(
      @AggregateWith(VarArgsAggregator.class) Object... args) {
    try (Recording recording = flightRecorderConnection.newRecording(null, null)) {
      reflectivelyInvokeMethods(recording, args);
    } catch (InvocationTargetException invocationTargetException) {
      assertTrue(invocationTargetException.getCause() instanceof IllegalStateException);
    } catch (Exception e) {
      fail("Bad test code", e);
    }
  }

  @Keep
  public static Stream<Arguments> assertRecordingOptionsAreSetInFlightRecorderMXBean() {
    return Stream.of(
        Arguments.of(""),
        Arguments.of("name=test"),
        Arguments.of("maxAge=30 s", "disk=true"),
        Arguments.of("maxSize=1048576", "disk=true"),
        Arguments.of("dumpOnExit=true"),
        Arguments.of("destination=temp.jfr", "disk=true"),
        Arguments.of("duration=30 s"),
        Arguments.of(
            "name=test",
            "maxAge=30 s",
            "maxSize=1048576",
            "dumpOnExit=true",
            "destination=temp.jfr",
            "disk=true",
            "duration=30 s"));
  }

  @ParameterizedTest
  @MethodSource
  @SuppressWarnings("unchecked")
  void assertRecordingOptionsAreSetInFlightRecorderMXBean(
      @AggregateWith(VarArgsAggregator.class) Object... options) {
    MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName flightRecorder = null;
    try {
      flightRecorder = new ObjectName("jdk.management.jfr:type=FlightRecorder");
    } catch (MalformedObjectNameException e) {
      // If the ObjectName is malformed, then the test is broken.
      fail("Something really bad happened", e);
    }

    RecordingOptions.Builder builder = new RecordingOptions.Builder();
    // Set values in the builder using reflection from the key=value of each arg
    try {
      for (Object opt : options) {
        String[] keyValue = ((String) opt).split("=");
        if (keyValue.length < 2) {
          continue;
        }
        String key = keyValue[0];
        String value = keyValue[1];
        Method method = RecordingOptions.Builder.class.getMethod(key, String.class);
        method.invoke(builder, value);
      }
    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException badData) {
      fail("Issue in test data: " + badData.getMessage());
    }

    RecordingOptions recordingOptions = builder.build();
    try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, null)) {
      long id = recording.start();
      TabularData flightRecorderMXBeanOptions =
          (TabularData)
              mBeanServer.invoke(
                  flightRecorder,
                  "getRecordingOptions",
                  new Object[] {id},
                  new String[] {long.class.getName()});
      assertFalse(flightRecorderMXBeanOptions.isEmpty());
      ((Collection<CompositeData>) flightRecorderMXBeanOptions.values())
          .forEach(
              compositeData -> {
                String key = (String) compositeData.get("key");
                String getter =
                    "get" + key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
                String expected = (String) compositeData.get("value");

                // Special case for duration values. The FlightRecorderMXBean wants "<number><unit>"
                // but returns "<number> <unit>", so we need to normalize the expected value.
                if (expected != null && expected.matches("([-+]?\\d+)\\s*(\\w*)")) {
                  expected = expected.replaceAll("\\s", "");
                }

                try {
                  Method method = RecordingOptions.class.getMethod(getter);
                  String actual = (String) method.invoke(recordingOptions);
                  // special case for name since FlightRecorderMXBean returns id as default
                  // and for destination since FlightRecorderMXBean returns null as default
                  if (!("name".equals(key) && "".equals(actual))
                      && !("destination".equals(key) && "".equals(actual))) {
                    assertEquals(expected, actual, getter);
                  }
                } catch (NoSuchMethodException
                    | IllegalArgumentException
                    | IllegalAccessException
                    | InvocationTargetException badAPI) {
                  fail("Issue in RecordingOptions API: " + badAPI.getMessage());
                }
              });
      recording.stop();
    } catch (IOException ioe) {
      fail("IOException not expected: ", ioe);
    } catch (IllegalArgumentException badData) {
      fail("Issue in test data: " + badData.getMessage());
    } catch (JfrConnectionException | ReflectionException | MBeanException badBean) {
      fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
    } catch (InstanceNotFoundException badJvm) {
      fail("Either JVM does not support JFR, or experimental options need to be enabled");
    } catch (RuntimeMBeanException ex) {
      // some versions of java don't support the 'destination' option
      if (!(ex.getCause() instanceof IllegalArgumentException)) {
        fail("Something bad happened", ex);
      }
    }
  }

  // something to do
  protected static void fib(int limit) {
    BigDecimal[] fibs = new BigDecimal[limit];
    fibs[0] = new BigDecimal(0);
    fibs[1] = new BigDecimal(1);
    for (int i = 2; i < fibs.length; i++) {
      fibs[i] = fibs[i - 1].add(fibs[i - 2]);
    }
  }

  @Test
  void assertFileExistsAfterRecordingDump() {
    RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
    try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, null)) {
      recording.start();
      Instant now = Instant.now();
      Instant then = now.plusSeconds(1);
      while (Instant.now().compareTo(then) < 0) {
        fib(Short.MAX_VALUE); // do something
      }
      recording.stop();
      Path dumpFile = Paths.get(System.getProperty("user.dir"), "testRecordingDump_dumped.jfr");
      recording.dump(dumpFile.toString());
      assertTrue(Files.exists(dumpFile));
    } catch (IllegalArgumentException badData) {
      fail("Issue in test data: " + badData.getMessage());
    } catch (IOException ioe) {
      // possible that this can be thrown, but should not happen in this context
      fail("IOException not expected: ", ioe);
    } catch (JfrConnectionException badBean) {
      fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
    }
  }

  @Test
  void assertFileExistsAfterRecordingStream() {
    RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
    try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, null)) {
      recording.start();
      Instant now = Instant.now();
      Instant then = now.plusSeconds(1);
      while (Instant.now().compareTo(then) < 0) {
        fib(Short.MAX_VALUE); // do something
      }
      recording.stop();

      Path streamedFile =
          Paths.get(System.getProperty("user.dir"), "testRecordingStream_getStream.jfr");
      try (InputStream inputStream = recording.getStream(now, then); // get the whole thing.
          OutputStream outputStream = new FileOutputStream(streamedFile.toFile())) {
        int c = -1;
        while ((c = inputStream.read()) != -1) {
          outputStream.write(c);
        }
      } catch (IOException e) {
        fail(e.getMessage(), e);
      }

      assertTrue(Files.exists(streamedFile));

    } catch (IllegalArgumentException badData) {
      fail("Issue in test data: " + badData.getMessage());
    } catch (IOException ioe) {
      fail("IOException not expected: ", ioe);
    } catch (JfrConnectionException badBean) {
      fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
    }
  }

  @Test
  void assertStreamedFileEqualsDumpedFile() {

    Path dumpedFile = Paths.get(System.getProperty("user.dir"), "testRecordingStream_dumped.jfr");
    ;
    Path streamedFile =
        Paths.get(System.getProperty("user.dir"), "testRecordingStream_getStream.jfr");
    ;
    RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
    try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, null)) {
      recording.start();
      Instant now = Instant.now();
      Instant then = now.plusSeconds(1);
      while (Instant.now().compareTo(then) < 0) {
        fib(Short.MAX_VALUE); // do something
      }
      recording.stop();
      recording.dump(dumpedFile.toString());

      try (InputStream inputStream = recording.getStream(now, then); // get the whole thing.
          OutputStream outputStream = new FileOutputStream(streamedFile.toFile())) {
        int c = -1;
        while ((c = inputStream.read()) != -1) {
          outputStream.write(c);
        }
      } catch (IOException e) {
        fail(e.getMessage(), e);
      }

      try (InputStream streamed = new FileInputStream(streamedFile.toFile());
          InputStream dumped = new FileInputStream(dumpedFile.toFile())) {
        int a = -1;
        int b = -1;
        do {
          a = streamed.read();
          b = dumped.read();
        } while (a != -1 && b != -1 && a == b);
        if (a != b) {
          fail(dumpedFile + " differs from " + streamedFile);
        }
      } catch (IOException e) {
        fail(e.getMessage(), e);
      }
      // if we get here, then the files compare the same
    } catch (IllegalArgumentException badData) {
      fail("Issue in test data: " + badData.getMessage());
    } catch (IOException ioe) {
      // possible that this can be thrown, but should not happen in this context
      fail("IOException not expected: ", ioe);
    } catch (JfrConnectionException badBean) {
      fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
    }
  }

  @Test
  void assertRecordingCloneState() {
    // Recording#clone returns a clone of the recording with the same state, but clone has its own
    // id.
    // Recording#clone with 'true' causes clone to close before returning.
    RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
    try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, null)) {
      recording.start();
      Recording clone = recording.clone(true);
      assertSame(recording.getState(), Recording.State.RECORDING);
      assertSame(clone.getState(), Recording.State.STOPPED);
      assertNotEquals(recording.getId(), clone.getId());
      recording.stop();
    } catch (IOException ioe) {
      // possible that this can be thrown, but should not happen in this context
      fail("IOException not expected: ", ioe);
    } catch (JfrConnectionException badBean) {
      fail("Error thrown by MBean server or FlightRecorderMXBean: ", badBean);
    }
  }
}
