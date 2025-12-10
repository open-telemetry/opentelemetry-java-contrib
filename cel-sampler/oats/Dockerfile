FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

# renovate: datasource=github-releases depName=open-telemetry/opentelemetry-java-instrumentation
ENV OPENTELEMETRY_JAVA_INSTRUMENTATION_VERSION=v2.22.0

# Add the test application JAR
COPY ./cel-sampler/testapp/build/libs/testapp.jar ./app.jar

# Add the CEL sampler extension JAR (shadow JAR with dependencies)
# Copy all shadow JARs to extensions directory
COPY ./cel-sampler/build/libs/*-shadow.jar ./extensions/

COPY ./cel-sampler/oats/otel-config.yaml ./otel-config.yaml

ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/$OPENTELEMETRY_JAVA_INSTRUMENTATION_VERSION/opentelemetry-javaagent.jar ./opentelemetry-javaagent.jar

# Configure the Java agent with the CEL sampler extension
ENV JAVA_TOOL_OPTIONS=-javaagent:./opentelemetry-javaagent.jar
ENV OTEL_JAVAAGENT_EXTENSIONS=/usr/src/app/extensions/

ENV OTEL_EXPERIMENTAL_CONFIG_FILE=./otel-config.yaml

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./app.jar" ]
