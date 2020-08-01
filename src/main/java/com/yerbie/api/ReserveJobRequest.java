package com.yerbie.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReserveJobRequest {
  private final String queue;

  @JsonCreator
  public ReserveJobRequest(@JsonProperty("queue") String queue) {
    this.queue = queue;
  }

  @JsonProperty
  public String getQueue() {
    return queue;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ReserveJobRequest)) return false;

    ReserveJobRequest otherReserveJobRequest = (ReserveJobRequest) other;

    return this.queue.equals(otherReserveJobRequest.queue);
  }
}
