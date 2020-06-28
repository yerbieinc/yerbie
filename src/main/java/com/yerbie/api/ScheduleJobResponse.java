package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleJobResponse {
  private final String jobToken;

  public ScheduleJobResponse(@JsonProperty("jobToken") String jobToken) {
    this.jobToken = jobToken;
  }

  @JsonProperty
  public String getJobToken() {
    return jobToken;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ScheduleJobResponse)) return false;

    ScheduleJobResponse otherScheduleJobResponse = (ScheduleJobResponse) other;

    return this.jobToken.equals(otherScheduleJobResponse.jobToken);
  }

  @Override
  public int hashCode() {
    return jobToken.hashCode();
  }
}
