package com.yerbie.core.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JobData {
  final String jobPayload;
  final long delaySeconds;
  final String queue;
  final String jobToken;

  @JsonCreator
  public JobData(
      @JsonProperty("jobPayload") final String jobPayload,
      @JsonProperty("delaySeconds") final long delaySeconds,
      @JsonProperty("queue") final String queue,
      @JsonProperty("jobToken") final String jobToken) {
    this.jobPayload = jobPayload;
    this.delaySeconds = delaySeconds;
    this.queue = queue;
    this.jobToken = jobToken;
  }

  @JsonProperty
  public String getJobPayload() {
    return jobPayload;
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
    if (!(other instanceof JobData)) return false;

    JobData otherScheduleJobRequest = (JobData) other;

    return this.delaySeconds == otherScheduleJobRequest.delaySeconds
        && this.jobPayload.equals(otherScheduleJobRequest.jobPayload)
        && this.queue.equals(otherScheduleJobRequest.queue);
  }

  @Override
  public int hashCode() {
    int result = 1;
    int prime = 31;
    result = prime * result + jobPayload.hashCode();
    result = prime * result + queue.hashCode();
    result = prime * result + jobToken.hashCode();
    return result;
  }
}
