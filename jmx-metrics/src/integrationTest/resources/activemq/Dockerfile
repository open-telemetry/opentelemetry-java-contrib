FROM rmohr/activemq:5.15.9@sha256:b5669f141ac5c455b1010b90d348fe78289bba7e2db5659484d99736b696593c


COPY --chown=activemq config/env /opt/activemq/bin/env
ENV ACTIVEMQ_JMX=10991
EXPOSE $ACTIVEMQ_JMX

ENV ACTIVEMQ_JMX_OPTS="-Dcom.sun.management.jmxremote.port=10991 -Dcom.sun.management.jmxremote.rmi.port=10991 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
