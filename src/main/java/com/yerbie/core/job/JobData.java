package com.yerbie.core.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JobData {
  final String jobPayload;
  final long delaySeconds;
  final String queue;
  final String jobToken;
  final int unackedRetries;

  @JsonCreator
  public JobData(
      @JsonProperty("jobPayload") final String jobPayload,
      @JsonProperty("delaySeconds") final long delaySeconds,
      @JsonProperty("queue") final String queue,
      @JsonProperty("jobToken") final String jobToken,
      @JsonProperty("unackedRetries") final int unackedRetries) {
    this.jobPayload = jobPayload;
    this.delaySeconds = delaySeconds;
    this.queue = queue;
    this.jobToken = jobToken;
    this.unackedRetries = unackedRetries;
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

  @JsonProperty
  public int getUnackedRetries() {
    return unackedRetries;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof JobData)) return false;

    JobData otherJobData = (JobData) other;

    return this.delaySeconds == otherJobData.delaySeconds
        && this.jobPayload.equals(otherJobData.jobPayload)
        && this.queue.equals(otherJobData.queue)
        && this.jobToken.equals(otherJobData.jobToken)
        && this.unackedRetries == otherJobData.unackedRetries;
  }

  @Override
  public int hashCode() {
    int result = 1;
    int prime = 31;
    result = prime * result + jobPayload.hashCode();
    result = prime * result + queue.hashCode();
    result = prime * result + jobToken.hashCode();
    result = prime * result + Long.valueOf(delaySeconds).intValue();
    result = prime * result + Long.valueOf(unackedRetries).intValue();
    return result;
  }
}
