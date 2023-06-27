package io.opentelemetry.contrib.disk.buffering.internal.serialization.mapping.common;

import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import javax.annotation.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ResourceMapper {

  public static final ResourceMapper INSTANCE = new ResourceMapperImpl();

  public Resource mapToProto(io.opentelemetry.sdk.resources.Resource sdkResource) {
    return Resource.newBuilder()
        .addAllAttributes(AttributesMapper.INSTANCE.attributesToProto(sdkResource.getAttributes()))
        .build();
  }

  public abstract io.opentelemetry.sdk.resources.Resource mapToSdk(
      Resource protoResource, @Nullable String schemaUrl);

  @AfterMapping
  protected void addAttributes(Resource protoResource, @MappingTarget ResourceBuilder builder) {
    builder.putAll(AttributesMapper.INSTANCE.protoToAttributes(protoResource.getAttributesList()));
  }
}
