package io.opentelemetry.contrib.disk.buffer.testutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.disk.buffer.internal.mapping.common.BaseResourceSignalsDataMapper;
import io.opentelemetry.contrib.disk.buffer.internal.serialization.common.ResourceSignalsData;
import java.util.Arrays;
import java.util.List;

public abstract class BaseSignalDataMapperTest<
    SIGNAL_SDK_ITEM, DTO_RESOURCE_DATA extends ResourceSignalsData<?>> {
  protected DTO_RESOURCE_DATA toDto(SIGNAL_SDK_ITEM... items) {
    return getMapper().toJsonDto(Arrays.asList(items));
  }

  protected List<SIGNAL_SDK_ITEM> fromDto(DTO_RESOURCE_DATA dto) {
    return getMapper().fromJsonDto(dto);
  }

  protected void assertItemsMapping(SIGNAL_SDK_ITEM... targets) {
    DTO_RESOURCE_DATA dto = toDto(targets);
    List<SIGNAL_SDK_ITEM> mappedBack = fromDto(dto);

    assertThat(mappedBack).containsExactly(targets);
  }

  protected abstract BaseResourceSignalsDataMapper<SIGNAL_SDK_ITEM, ?, ?, ?, DTO_RESOURCE_DATA>
      getMapper();
}
