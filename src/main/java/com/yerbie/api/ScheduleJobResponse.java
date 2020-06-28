package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleJobResponse {
  private final String jobToken;

  public ScheduleJobResponse(String jobToken) {
    this.jobToken = jobToken;
  }

  @JsonProperty
  public String getJobToken() {
    return jobToken;
  }
}
