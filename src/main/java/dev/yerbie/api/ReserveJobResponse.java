package dev.yerbie.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReserveJobResponse {
  private final long delaySeconds;
  private final String jobData;
  private final String queue;
  private final String jobToken;

  @JsonCreator
  public ReserveJobResponse(
      @JsonProperty("delaySeconds") long delaySeconds,
      @JsonProperty("jobData") String jobData,
      @JsonProperty("queue") String queue,
      @JsonProperty("jobToken") String jobToken) {
    this.jobData = jobData;
    this.delaySeconds = delaySeconds;
    this.queue = queue;
    this.jobToken = jobToken;
  }

  @JsonProperty
  public String getJobData() {
    return jobData;
  }

  @JsonProperty
  public long getDelaySeconds() {
    return delaySeconds;
  }

  @JsonProperty
  public String getQueue() {
    return queue;
  }

  @JsonProperty
  public String getJobToken() {
    return jobToken;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ReserveJobResponse)) return false;

    ReserveJobResponse otherReserveJobResponse = (ReserveJobResponse) other;

    return this.delaySeconds == otherReserveJobResponse.delaySeconds
        && this.jobData.equals(otherReserveJobResponse.jobData)
        && this.queue.equals(otherReserveJobResponse.queue);
  }

  @Override
  public int hashCode() {
    return jobData.hashCode() + queue.hashCode();
  }
}
