package io.opentelemetry.contrib.disk.buffer.serialization.metrics.datapoints.data;

import com.dslplatform.json.JsonAttribute;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public final class Buckets {

  @Nullable
  @JsonAttribute(name = "offset")
  public Integer offset;

  @JsonAttribute(name = "bucketCounts")
  public List<String> bucketCounts = new ArrayList<>();

  // Not present in the proto
  @Nullable
  @JsonAttribute(name = "scale")
  public Integer scale;

  @Nullable
  @JsonAttribute(name = "totalCount")
  public String totalCount;
}
