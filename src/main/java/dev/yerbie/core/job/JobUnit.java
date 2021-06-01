package dev.yerbie.core.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public class JobUnit {
  final JsonNode serializedJobData;

  final String timestampBucketKey;

  @JsonCreator
  public JobUnit(
      @JsonProperty("serializedJobData") JsonNode serializedJobData,
      @JsonProperty("timestampBucketKey") final String timestampBucketKey) {
    this.serializedJobData = serializedJobData;
    this.timestampBucketKey = timestampBucketKey;
  }

  @JsonProperty
  public JsonNode getSerializedJobData() {
    return serializedJobData;
  }

  @JsonProperty
  public String getTimestampBucketKey() {
    return timestampBucketKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JobUnit jobUnit = (JobUnit) o;
    return serializedJobData.equals(jobUnit.serializedJobData)
        && timestampBucketKey.equals(jobUnit.timestampBucketKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serializedJobData, timestampBucketKey);
  }
}
