package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FinishJobResponse {
  private final String jobToken;

  @JsonCreator
  public FinishJobResponse(@JsonProperty("jobToken") String jobToken) {
    this.jobToken = jobToken;
  }

  @JsonProperty
  public String getJobToken() {
    return jobToken;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof FinishJobResponse)) return false;

    FinishJobResponse otherFinishJobResponse = (FinishJobResponse) other;

    return this.jobToken.equals(otherFinishJobResponse.jobToken);
  }

  @Override
  public int hashCode() {
    return jobToken.hashCode();
  }
}
