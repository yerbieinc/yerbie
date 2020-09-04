package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleJobRequest {
  private final long delaySeconds;
  private final String jobData;
  private final String queue;
  private final String jobToken;

  @JsonCreator
  public ScheduleJobRequest(
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
    if (!(other instanceof ScheduleJobRequest)) return false;

    ScheduleJobRequest otherScheduleJobRequest = (ScheduleJobRequest) other;

    return this.delaySeconds == otherScheduleJobRequest.delaySeconds
        && this.jobData.equals(otherScheduleJobRequest.jobData)
        && this.queue.equals(otherScheduleJobRequest.queue)
        && this.jobToken.equals(otherScheduleJobRequest.jobToken);
  }

  @Override
  public int hashCode() {
    return jobData.hashCode() + queue.hashCode() + jobToken.hashCode();
  }
}
