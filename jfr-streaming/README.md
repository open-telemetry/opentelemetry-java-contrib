# jfrs-otlp
JFR Streaming to OTLP Bridge

* Build it with ./gradlew sJ and you'll get a jar that can be used as an agent. 

* Java 17 only.

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