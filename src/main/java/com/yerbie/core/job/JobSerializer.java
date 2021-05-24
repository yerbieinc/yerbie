package com.yerbie.core.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public class JobSerializer {
  private final ObjectMapper objectMapper;

  public JobSerializer(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String serializeJob(JobData jobData) throws IOException {
    return objectMapper.writeValueAsString(jobData);
  }

  public JobData deserializeJob(String jobData) throws IOException {
    return objectMapper.readValue(jobData, JobData.class);
  }

  public String serializeJobUnit(JobUnit jobUnit) throws IOException {
    return objectMapper.writeValueAsString(jobUnit);
  }

  public JobUnit deserializeJobUnit(String serializedJobUnit) throws IOException {
    return objectMapper.readValue(serializedJobUnit, JobUnit.class);
  }

  public JsonNode jobDataToJSONNode(String jobData) throws IOException {
    return objectMapper.readTree(jobData);
  }
}
