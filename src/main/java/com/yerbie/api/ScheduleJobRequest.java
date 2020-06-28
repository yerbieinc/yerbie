package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleJobRequest {
  private final long delaySeconds;
  private final String jobData;

  @JsonCreator
  public ScheduleJobRequest(
      @JsonProperty("delaySeconds") long delaySeconds, @JsonProperty("jobData") String jobData) {
    this.jobData = jobData;
    this.delaySeconds = delaySeconds;
  }

  @JsonProperty
  public String getJobData() {
    return jobData;
  }

  @JsonProperty
  public long getDelaySeconds() {
    return delaySeconds;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ScheduleJobRequest)) return false;

    ScheduleJobRequest otherScheduleJobRequest = (ScheduleJobRequest) other;

    if (this.delaySeconds != otherScheduleJobRequest.delaySeconds) {
      return false;
    }

    return this.jobData.equals(otherScheduleJobRequest.jobData);
  }

  @Override
  public int hashCode() {
    return jobData.hashCode();
  }
}
