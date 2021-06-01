package dev.yerbie.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JobTokenResponse {
  private final String jobToken;

  @JsonCreator
  public JobTokenResponse(@JsonProperty("jobToken") String jobToken) {
    this.jobToken = jobToken;
  }

  @JsonProperty
  public String getJobToken() {
    return jobToken;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof JobTokenResponse)) return false;

    JobTokenResponse otherJobTokenResponse = (JobTokenResponse) other;

    return this.jobToken.equals(otherJobTokenResponse.jobToken);
  }

  @Override
  public int hashCode() {
    return jobToken.hashCode();
  }
}
