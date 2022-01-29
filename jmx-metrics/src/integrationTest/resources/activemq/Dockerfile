FROM rmohr/activemq:5.15.9-alpine


COPY --chown=activemq config/env /opt/activemq/bin/env
ENV ACTIVEMQ_JMX=10991
EXPOSE $ACTIVEMQ_JMX

ENV ACTIVEMQ_JMX_OPTS="-Dcom.sun.management.jmxremote.port=10991 -Dcom.sun.management.jmxremote.rmi.port=10991 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
