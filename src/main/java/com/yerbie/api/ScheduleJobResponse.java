package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleJobResponse {
  private final String jobToken;
  private final String queue;
  private final String jobData;
  private final long delaySeconds;

  public ScheduleJobResponse(
      @JsonProperty("jobToken") String jobToken,
      @JsonProperty("queue") String queue,
      @JsonProperty("jobData") String jobData,
      @JsonProperty("delaySeconds") long delaySeconds) {
    this.jobToken = jobToken;
    this.queue = queue;
    this.jobData = jobData;
    this.delaySeconds = delaySeconds;
  }

  public String getJobToken() {
    return jobToken;
  }

  public String getQueue() {
    return queue;
  }

  public String getJobData() {
    return jobData;
  }

  public long getDelaySeconds() {
    return delaySeconds;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ScheduleJobResponse)) return false;

    ScheduleJobResponse otherScheduleJobResponse = (ScheduleJobResponse) other;

    return this.delaySeconds == otherScheduleJobResponse.delaySeconds
        && this.jobData.equals(otherScheduleJobResponse.jobData)
        && this.queue.equals(otherScheduleJobResponse.queue)
        && this.jobToken.equals(otherScheduleJobResponse.jobToken);
  }

  @Override
  public int hashCode() {
    return jobData.hashCode() + queue.hashCode() + jobToken.hashCode();
  }
}
