package com.yerbie.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Internal representation of a job sent for scheduling by a client. */
public class Job {
  private final long delaySeconds;
  private final String jobData;

  public Job(
      @JsonProperty("delaySeconds") final long delaySeconds,
      @JsonProperty("jobData") final String jobData) {
    this.delaySeconds = delaySeconds;
    this.jobData = jobData;
  }

  @JsonProperty
  public long getDelaySeconds() {
    return delaySeconds;
  }

  @JsonProperty
  public String getJobData() {
    return jobData;
  }
}
