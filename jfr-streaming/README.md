# jfrs-otlp
JFR Streaming to OTLP Bridge

* Java 17 only.

* You need to run an initial bootstrapping build:

```
./gradlew :jfr-streaming:build -x test
```

* This skips the tests (which themselves run with an agent installed) and gives a jar that can be used as an agent.

* Move it to `jfr-streaming.jar` in the root of the jfr-streaming subproject so that the tests can find it

* Rebuild it with `./gradlew :jfr-streaming:build`

Export a couple of env vars - OTLP_URL and API_KEY

Then run it something like this:
  
```
java -Xmx1G -Djava.util.logging.config.file=log.properties -javaagent:jfr-streaming-1.6.0-SNAPSHOT.jar -jar target/HyperAlloc-1.0.jar -a 1 -h 1 -d 3600
```

You'll need a JUL log.properties file, something like this:

```
handlers=java.util.logging.ConsoleHandler
.level=FINE
java.util.logging.ConsoleHandler.level=ALL
```