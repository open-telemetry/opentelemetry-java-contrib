# JFR Connection

The `jfr-connection` module provides a core library for configuring, starting, stopping,
and reading [Java Flight Recording](https://docs.oracle.com/en/java/java-components/jdk-mission-control/8/user-guide/using-jdk-flight-recorder.html#GUID-D38849B6-61C7-4ED6-A395-EA4BC32A9FD6)
files from a JVM. The code does not depend on the `jdk.jfr`
module and will compile and run against JDK 8 or higher. It uses a connection to an MBean
server, which can be the platform MBean server, or a remote MBean server connected by
means of JMX.

* Java 8+
* Build with `./gradlew :jfr-connection:build`

The main entry point is `io.opentelemetry.contrib.jfr.connection.FlightRecorderConnection`:

```java
  // The MBeanServerConnection can be local or remote
  MBeanServerConnection mBeanServer = ManagementFactory.getPlatformMBeanServer();

  FlightRecorderConnection flightRecorderConnection = FlightRecorderConnection.connect(mBeanServer);
  RecordingOptions recordingOptions = new RecordingOptions.Builder().disk("true").build();
  RecordingConfiguration recordingConfiguration = RecordingConfiguration.PROFILE_CONFIGURATION;

  try (Recording recording = flightRecorderConnection.newRecording(recordingOptions, recordingConfiguration)) {
      recording.start();
      TimeUnit.SECONDS.sleep(10);
      recording.stop();

      recording.dump(Paths.get(System.getProperty("user.dir"), "recording.jfr").toString());
  }
```

---
Note on Oracle JDK 8:

For Oracle JDK 8, it may be necessary to unlock the Java Flight Recorder
commercial feature with the JVM arg `-XX:+UnlockCommercialFeatures -XX:+FlightRecorder`.
Starting with JDK 8u262, Java Flight Recorder is available for all OpenJDK distributions.

---

## Component owners

- [Trask Stalnaker](https://github.com/trask), Microsoft
- [Jason Plumb](https://github.com/breedx-splk), Splunk
- [Jean Bisutti](https://github.com/jeanbisutti), Microsoft
- [David Grieve](https://github.com/dsgrieve), Microsoft

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
