package io.opentelemetry.contrib.disk.buffer.internal.mapping.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mapstruct.Mapping;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Mapping(target = "schemaUrl", source = "source.schemaUrl")
@Mapping(target = "scope", source = "source")
@Mapping(target = "signalItems", ignore = true)
public @interface ScopeMapping {}
