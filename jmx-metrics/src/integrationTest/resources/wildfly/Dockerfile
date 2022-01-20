FROM jboss/wildfly:23.0.1.Final

EXPOSE 9990

RUN /opt/jboss/wildfly/bin/add-user.sh cassandra cassandra --silent

COPY --chown=jboss:root start.sh /start.sh

ENTRYPOINT ["sh", "/start.sh"]