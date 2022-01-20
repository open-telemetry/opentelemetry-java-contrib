FROM dajobe/hbase

# JMX
EXPOSE 9900

RUN echo "export HBASE_JMX_BASE=\"-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false\"" >> /opt/hbase/conf/hbase-env.sh
RUN echo "export HBASE_MASTER_OPTS=\"\$HBASE_MASTER_OPTS \$HBASE_JMX_BASE -Dcom.sun.management.jmxremote.rmi.port=9900 -Dcom.sun.management.jmxremote.port=9900\"" >> /opt/hbase/conf/hbase-env.sh