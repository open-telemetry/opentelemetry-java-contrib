package io.opentelemetry.contrib.jfr.metrics;

import io.opentelemetry.api.common.AttributeKey;

public class Constants {
    private Constants() {}

    public static final String KILOBYTES = "KB";
    public static final String MILLISECONDS = "ms";
    public static final String PERCENTAGE = "%age";
    public static final AttributeKey<String> ATTR_THREAD_NAME = AttributeKey.stringKey("thread.name");
}
