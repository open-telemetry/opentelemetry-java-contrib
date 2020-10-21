/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.contrib.jmxmetrics

import javax.management.MBeanServerConnection
import javax.management.ObjectName
import java.util.logging.Logger

/**
 * A helper for easy {@link GroovyMBean} querying, creation, and protected attribute access.
 *
 * Basic query functionality is provided by the static queryJmx methods, while MBeanHelper
 * instances operate on an underlying GroovyMBean list field.
 *
 * Specifying if a single underlying GroovyMBean is expected or of interest is done via the
 * isSingle argument, where resulting attribute values are only returned for the first
 * match.
 *
 * Intended to be used via the script-bound `otel` instance:
 *
 * def singleMBean = otel.mbean("com.example:type=SingleType")
 * def multipleMBeans = otel.mbeans("com.example:type=MultipleType,*")
 * [singleMBean, multipleMBeans].each { it.fetch() }
 *
 */
class MBeanHelper {
    private static final Logger logger = Logger.getLogger(MBeanHelper.class.getName());

    private List<GroovyMBean> mbeans
    protected JmxClient jmxClient
    protected boolean isSingle

    private String objectName

    MBeanHelper(JmxClient jmxClient, String objectName, boolean isSingle) {
        this.jmxClient = jmxClient
        this.objectName = objectName
        this.isSingle = isSingle
    }

    static List<GroovyMBean> queryJmx(JmxClient jmxClient, String objNameStr) {
        return queryJmx(jmxClient, new ObjectName(objNameStr))
    }

    static List<GroovyMBean> queryJmx(JmxClient jmxClient, ObjectName objName) {
        List<ObjectName> names = jmxClient.query(objName)
        MBeanServerConnection server = jmxClient.connection
        return names.collect { new GroovyMBean(server, it) }
    }

    void fetch() {
        mbeans = queryJmx(jmxClient, objectName)
        if (mbeans.size() == 0) {
            logger.warning("Failed to fetch MBean ${objectName}.")
        } else {
            logger.fine("Fetched ${mbeans.size()} MBeans - ${mbeans}")
        }
    }

    List<GroovyMBean> getMBeans() {
        if (mbeans == null) {
            logger.warning("No active MBeans.  Be sure to fetch() before updating any applicable instruments.")
            return []
        }
        return mbeans
    }

    protected List<Object> getAttribute(String attribute) {
        if (mbeans == null || mbeans.size() == 0) {
            return []
        }

        if (isSingle) {
            return [
                mbeans[0].getProperty(attribute)
            ]
        }

        return mbeans.collect {
            it.getProperty(attribute)
        }
    }
}
