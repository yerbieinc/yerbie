package dev.yerbie.core.exception;

public class DuplicateJobException extends Exception {
  private String jobToken;

  public DuplicateJobException(String jobToken) {
    super(String.format("Job with jobToken %s already exists.", jobToken));
    this.jobToken = jobToken;
  }

  public String getJobToken() {
    return jobToken;
  }
}
