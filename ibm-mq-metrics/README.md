# IBM MQ Metrics

:warning: This software is under development.

## Use case

IBM MQ, formerly known as WebSphere MQ (message queue) series, is an IBM software for
program-to-program messaging across multiple platforms.

The IBM MQ metrics utility here can monitor multiple queues managers and their resources,
namely queues, topics, channels and listeners The metrics are extracted out using the
[PCF command messages](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_8.0.0/com.ibm.mq.adm.doc/q020010_.htm).

The metrics for queue manager, queue, topic, channel and listener can be configured.

The MQ Monitor is compatible with IBM MQ version 7.x, 8.x and 9.x.

## Prerequisites

This software requires compilation with Java 11.
It targets language level 8 and outputs java 8 class files.

The extension has a dependency on the following jar's depending on IBM MQ version:

* v8.0.0 and above

```
com.ibm.mq.allclient.jar
```

* For other versions

```
com.ibm.mq.commonservices.jar
com.ibm.mq.jar
com.ibm.mq.jmqi.jar
com.ibm.mq.headers.jar
com.ibm.mq.pcf.jar
dhbcore.jar
connector.jar
```

These jar files are typically found in ```/opt/mqm/java/lib``` on a UNIX server but may be
found in an alternate location depending upon your environment.

In case of **CLIENT** transport type, IBM MQ Client must be installed to get the MQ jars.
[The IBM MQ Client jars can be downloaded here](https://developer.ibm.com/messaging/mq-downloads/).

### MQ monitoring configuration

This software reads events from event queues associated with the queue manager:

* `SYSTEM.ADMIN.PERFM.EVENT`: Performance events, such as low, high, and full queue depth events.
* `SYSTEM.ADMIN.QMGR.EVENT`: Authority events
* `SYSTEM.ADMIN.CONFIG.EVENT`: Configuration events

Please turn on those events to take advantage of this monitoring.

## Build

Build the package with:

```shell
./gradlew shadowJar
```

Note: Due to restrictive licensing, this uber-jar (fat-jar) does not include the IBM client jar f

## Run

Run the standalone jar alongside the IBM jar:

```shell
java \
   -Djavax.net.ssl.keyStore=key.jks \
   -Djavax.net.ssl.keyStorePassword=<password> \
   -Djavax.net.ssl.trustStore=key.jks \
   -Djavax.net.ssl.trustStorePassword=<password> \
   -cp target/ibm-mq-monitoring-<version>-all.jar:lib/com.ibm.mq.allclient.jar \
   io.opentelemetry.ibm.mq.opentelemetry.Main \
   ./my-config.yml
```

## Connection

There are two transport modes in which this extension can be run:

* **Binding** : Requires WMQ Extension to be deployed in machine agent on the same machine where
  WMQ server is installed.
* **Client** : In this mode, the WMQ extension is installed on a different host than the IBM MQ
  server. Please install the [IBM MQ Client](https://developer.ibm.com/messaging/mq-downloads/)
  for this mode to get the necessary jars as mentioned previously.

If this extension is configured for **CLIENT** transport type
1. Please make sure the MQ's host and port is accessible.
2. Credentials of user with correct access rights would be needed in config.yml
  [(Access Permissions section)](https://github.com/signalfx/opentelemetry-ibm-mq-monitoring-extension#access-permissions).
3. If the hosting OS for IBM MQ is Windows, Windows user credentials will be needed.

If you are in **Bindings** mode, please make sure to start the MA process under a user which has
the following permissions on the broker. Similarly, for **Client** mode, please provide the user
credentials in config.yml which have permissions listed below.

The user connecting to the queueManager should have the inquire, get, put (since PCF responses
cause dynamic queues to be created) permissions. For metrics that execute MQCMD_RESET_Q_STATS
command, chg permission is needed.

### SSL Support

1. Configure the IBM SSL Cipher Suite in the config.yml.
   Note that, to use some CipherSuites the unrestricted policy needs to be configured in JRE.
   Please visit [this link](http://www.ibm.com/support/knowledgecenter/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/sdkpolicyfiles.html)
   for more details. For Oracle JRE, please update with [JCE Unlimited Strength Jurisdiction Policy](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html).
   The download includes a readme file with instructions on how to apply these files to JRE

2. Please add the following JVM arguments to the MA start up command or script.

   ```-Dcom.ibm.mq.cfg.useIBMCipherMappings=false```  (If you are using IBM Cipher Suites, set the
   flag to true. Please visit [this link](http://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.dev.doc/q113210_.htm) for more details.
   )
3. To configure SSL, the MA's trust store and keystore needs to be setup with the JKS filepath.
   They can be passed either as Machine Agent JVM arguments or configured in config.yml (sslConnection) <br />

   a. Machine Agent JVM arguments as follows:

   ```-Djavax.net.ssl.trustStore=<PATH_TO_JKS_FILE>```<br />
   ```-Djavax.net.ssl.trustStorePassword=<PASS>```<br />
   ```-Djavax.net.ssl.keyStore=<PATH_TO_JKS_FILE>```<br />
   ```-Djavax.net.ssl.keyStorePassword=<PASS>```<br />

   b. sslConnection in config.yml, configure the trustStorePassword. Same holds for keyStore configuration as well.

    ```
    sslConnection:
      trustStorePath: ""
      trustStorePassword: ""

      keyStorePath: ""
      keyStorePassword: ""
    ```

## Configuration

**Note** : Please make sure to not use tab (\t) while editing yaml files. You may want to validate
the yaml file using a [yaml validator](https://jsonformatter.org/yaml-validator). Configure the monitor by copying and editing the
config.yml file in <code>src/main/resources/config.yml</code>.

1. Configure the queueManagers with appropriate fields and filters. You can configure multiple
   queue managers in one configuration file.
2. To run the extension at a frequency > 1 minute, please configure the taskSchedule section.
   Refer to the [Task Schedule](https://community.appdynamics.com/t5/Knowledge-Base/Task-Schedule-for-Extensions/ta-p/35414) doc for details.

### Monitoring Workings - Internals

This software extracts metrics through [PCF framework](https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.adm.doc/q019990_.htm).
[A complete list of PCF commands are listed here](https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.adm.doc/q086870_.htm).
Each queue manager has an administration queue with a standard queue name and
the extension sends PCF command messages to that queue. On Windows and Unix platforms, the PCF
commands are sent is always sent to the SYSTEM.ADMIN.COMMAND.QUEUE queue.
[More details mentioned here](https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.adm.doc/q020010_.htm)

By default, the PCF responses are sent to the SYSTEM.DEFAULT.MODEL.QUEUE. Using this queue causes
a temporary dynamic queue to be created. You can override the default here by using the
`modelQueueName` and `replyQueuePrefix` fields in the config.yml.
[More details mentioned here](https://www.ibm.com/support/knowledgecenter/SSFKSJ_7.5.0/com.ibm.mq.ref.adm.doc/q083240_.htm)

## Metrics

See [docs/metrics.md](docs/metrics.md).

## Troubleshooting

1. Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-to-troubleshoot-missing-custom-metrics-or-extensions-metrics/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension.
2. Error `Completion Code '2', Reason '2495'`
   Normally this error occurs if the environment variables are not set up correctly for this extension to work MQ in Bindings Mode.

   If you are seeing `Failed to load the WebSphere MQ native JNI library: 'mqjbnd'`, please add the following jvm argument when starting the MA.

   -Djava.library.path=\<path to libmqjbnd.so\> For eg. on Unix it could -Djava.library.path=/opt/mqm/java/lib64 for 64-bit or -Djava.library.path=/opt/mqm/java/lib for 32-bit OS

   Sometimes you also have run the setmqenv script before using the above jvm argument to start the machine agent.

   . /opt/mqm/bin/setmqenv -s

   For more details, please check this [doc](https://www.ibm.com/support/knowledgecenter/en/SSFKSJ_7.1.0/com.ibm.mq.doc/zr00610_.htm)

   This might occur due to various reasons ranging from incorrect installation to applying [ibm fix packs](http://www-01.ibm.com/support/docview.wss?uid=swg21410038) but most of the time it happens when you are trying to connect in `Bindings` mode and machine agent is not on the same machine on which WMQ server is running. If you want to connect to WMQ server from a remote machine then connect using `Client` mode.

   Another way to get around this issue is to avoid using the Bindings mode. Connect using CLIENT transport type from a remote box.

3. Error `Completion Code '2', Reason '2035'`
   This could happen for various reasons but for most of the cases, for **Client** mode the user specified in config.yml is not authorized to access the queue manager. Also sometimes even if userid and password are correct, channel auth (CHLAUTH) for that queue manager blocks traffics from other ips, you need to contact admin to provide you access to the queue manager.
   For Bindings mode, please make sure that the MA is owned by a mqm user. Please check [this doc](https://www-01.ibm.com/support/docview.wss?uid=swg21636093)

4. `MQJE001: Completion Code '2', Reason '2195'`
   This could happen in **Client** mode. Please make sure that the IBM MQ dependency jars are correctly referenced in classpath of monitor.xml

5. `MQJE001: Completion Code '2', Reason '2400'`
   This could happen if unsupported cipherSuite is provided or JRE not having/enabled unlimited jurisdiction policy files. Please check SSL Support section.

6. If you are seeing "NoClassDefFoundError" or "ClassNotFound" error for any of the MQ dependency even after providing correct path in monitor.xml, then you can also try copying all the required jars in WMQMonitor (MAHome/monitors/WMQMonitor) folder and provide classpath in monitor.xml like below

   ```
    <classpath>ibm-mq-monitoring-<version>-all.jar;com.ibm.mq.allclient.jar</classpath>
   ```

   OR

   ```
    <classpath>ibm-mq-monitoring-<version>-all.jar;com.ibm.mq.jar;com.ibm.mq.jmqi.jar;com.ibm.mq.commonservices.jar;com.ibm.mq.headers.jar;com.ibm.mq.pcf.jar;connector.jar;dhbcore.jar</classpath>
   ```

## Component Owners

- [Antoine Toulme Sharma](https://github.com/atoulme), Splunk
- [Jason Plumb](https://github.com/breedx-splk), Splunk

Learn more about component owners in [component_owners.yml](../.github/component_owners.yml).
