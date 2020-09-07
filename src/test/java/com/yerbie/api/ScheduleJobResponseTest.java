package com.yerbie.api;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleJobResponseTest {

  private final ObjectMapper objectMapper = Jackson.newObjectMapper();
  private static final String JSON_SCHEDULE_JOB_RESPONSE =
      "{\"jobToken\":\"jobToken\",\"queue\":\"queue\",\"jobData\":\"jobData\",\"delaySeconds\":10}";
  private static final ScheduleJobResponse SCHEDULE_JOB_RESPONSE =
      new ScheduleJobResponse("jobToken", "queue", "jobData", 10);

  @Test
  public void testDeserialize() throws Exception {
    assertEquals(
        SCHEDULE_JOB_RESPONSE,
        objectMapper.readValue(JSON_SCHEDULE_JOB_RESPONSE, ScheduleJobResponse.class));
  }

  @Test
  public void testSerialize() throws Exception {
    assertEquals(
        JSON_SCHEDULE_JOB_RESPONSE, objectMapper.writeValueAsString(SCHEDULE_JOB_RESPONSE));
  }
}
