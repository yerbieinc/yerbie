package com.yerbie.stub;

import com.yerbie.core.job.JobData;
import java.time.Instant;

public class StubData {
  public static Instant AUGUST_ONE_INSTANT = Instant.ofEpochSecond(1596318740);
  public static JobData SAMPLE_JOB_DATA = new JobData("JOB_PAYLOAD", 10, "queue", "TOKEN");
  public static String SAMPLE_JOB_DATA_STRING =
      "{\"jobPayload\":\"JOB_PAYLOAD\",\"delaySeconds\":10,\"queue\":\"queue\",\"jobToken\":\"TOKEN\"}";
}
