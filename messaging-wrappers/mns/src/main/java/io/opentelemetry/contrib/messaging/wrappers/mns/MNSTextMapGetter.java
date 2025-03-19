package io.opentelemetry.contrib.messaging.wrappers.mns;

import static java.util.Collections.emptyList;

import com.aliyun.mns.model.*;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MNSTextMapGetter<REQUEST extends MNSProcessRequest> implements TextMapGetter<REQUEST> {

    public static <REQUEST extends MNSProcessRequest> TextMapGetter<REQUEST> create() {
        return new MNSTextMapGetter<>();
    }

    @Override
    public Iterable<String> keys(REQUEST carrier) {
        if (carrier == null || carrier.getMessage() == null) {
            return emptyList();
        }
        Message message = carrier.getMessage();

        Map<String, MessageSystemPropertyValue> systemProperties = message.getSystemProperties();
        if (systemProperties == null) {
            systemProperties = Collections.emptyMap();
        }
        Map<String, MessagePropertyValue> userProperties = message.getUserProperties();
        if (userProperties == null) {
            userProperties = Collections.emptyMap();
        }
        List<String> keys = new ArrayList<>(systemProperties.size() + userProperties.size());
        keys.addAll(systemProperties.keySet());
        keys.addAll(userProperties.keySet());
        return keys;
    }

    @Override
    public String get(REQUEST carrier, String key) {
        if (carrier == null || carrier.getMessage() == null) {
            return null;
        }
        Message message = carrier.getMessage();

        // the system property should always take precedence over the user property
        MessageSystemPropertyName systemPropertyName = convert2SystemPropertyName(key);
        if (systemPropertyName != null) {
            MessageSystemPropertyValue value = message.getSystemProperty(systemPropertyName);
            if (value != null) {
                return value.getStringValueByType();
            }
        }

        Map<String, MessagePropertyValue> userProperties = message.getUserProperties();
        if (userProperties == null || userProperties.isEmpty()) {
            return null;
        }
        MessagePropertyValue value = userProperties.getOrDefault(key, null);
        if (value != null) {
            return value.getStringValueByType();
        }
        return null;
    }

    /**
     * see {@link MessageSystemPropertyName}
     * */
    private MessageSystemPropertyName convert2SystemPropertyName(String name) {
        if (name == null) {
            return null;
        } else if (name.equals(MessageSystemPropertyName.TRACE_PARENT.getValue())) {
            return MessageSystemPropertyName.TRACE_PARENT;
        } else if (name.equals(MessageSystemPropertyName.BAGGAGE.getValue())) {
            return MessageSystemPropertyName.BAGGAGE;
        } else if (name.equals(MessageSystemPropertyName.TRACE_STATE.getValue())) {
            return MessageSystemPropertyName.TRACE_STATE;
        }
        return null;
    }

    private MNSTextMapGetter() {
    }
}
