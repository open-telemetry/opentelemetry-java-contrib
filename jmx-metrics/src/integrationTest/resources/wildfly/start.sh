#!/bin/sh

/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0 & \

/opt/jboss/wildfly/bin/add-user.sh user password --silent & \

java -cp /app/OpenTelemetryJMXMetrics.jar:/opt/jboss/wildfly/bin/client/jboss-client.jar \
  -Dotel.jmx.username=user -Dotel.jmx.password=password \
  -Dotel.exporter.otlp.endpoint=$OTLP_ENDPOINT \
  io.opentelemetry.contrib.jmxmetrics.JmxMetrics -config /app/target-systems/wildfly.properties
