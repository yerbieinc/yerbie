package com.yerbie.api;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleJobRequestTest {

  private final ObjectMapper objectMapper = Jackson.newObjectMapper();
  private static final String JSON_SCHEDULE_JOB_REQUEST =
      "{\"delaySeconds\":100,\"jobData\":\"jobData\",\"queue\":\"jobQueue\"}";

  private static final ScheduleJobRequest SCHEDULE_JOB_REQUEST =
      new ScheduleJobRequest(100, "jobData", "jobQueue");

  @Test
  public void testSerialize() throws Exception {
    assertEquals(JSON_SCHEDULE_JOB_REQUEST, objectMapper.writeValueAsString(SCHEDULE_JOB_REQUEST));
  }

  @Test
  public void testDeserialize() throws Exception {
    assertEquals(
        SCHEDULE_JOB_REQUEST,
        objectMapper.readValue(JSON_SCHEDULE_JOB_REQUEST, ScheduleJobRequest.class));
  }
}
