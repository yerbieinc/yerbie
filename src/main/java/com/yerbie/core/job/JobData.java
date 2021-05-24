package com.yerbie.core.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JobData jobData = (JobData) o;
    return delaySeconds == jobData.delaySeconds
        && unackedRetries == jobData.unackedRetries
        && jobPayload.equals(jobData.jobPayload)
        && queue.equals(jobData.queue)
        && jobToken.equals(jobData.jobToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobPayload, delaySeconds, queue, jobToken, unackedRetries);
  }
}
