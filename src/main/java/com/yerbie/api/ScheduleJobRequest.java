package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleJobRequest {
  private final String jobData;
  private final long delaySeconds;

  public ScheduleJobRequest(String jobData, long delaySeconds) {
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
}
