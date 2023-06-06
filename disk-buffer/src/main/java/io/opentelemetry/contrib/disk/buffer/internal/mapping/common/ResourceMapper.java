package io.opentelemetry.contrib.disk.buffer.internal.mapping.common;

import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ResourceJson;
import javax.annotation.Nullable;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public abstract class ResourceMapper {

  public static final ResourceMapper INSTANCE = Mappers.getMapper(ResourceMapper.class);

  public abstract ResourceJson resourceToJson(io.opentelemetry.sdk.resources.Resource source);

  public io.opentelemetry.sdk.resources.Resource jsonToResource(
      ResourceJson source, @Context @Nullable String schemaUrl) {
    return io.opentelemetry.sdk.resources.Resource.create(source.attributes, schemaUrl);
  }
}