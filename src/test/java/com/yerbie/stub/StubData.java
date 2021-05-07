package com.yerbie.stub;

import com.yerbie.core.job.JobData;
import java.time.Instant;

public class StubData {
  public static Long AUGUST_ONE_LONG_EPOCH_SECOND = 1596318740L;
  public static Instant AUGUST_ONE_INSTANT = Instant.ofEpochSecond(AUGUST_ONE_LONG_EPOCH_SECOND);
  public static String AUGUST_ONE_DELAYED_ITEM_LIST = "delayed_jobs_1596318740";
  public static JobData SAMPLE_JOB_DATA = new JobData("JOB_PAYLOAD", 10, "queue", "TOKEN", 0);
  public static JobData SAMPLE_JOB_DATA_WITH_RETRY =
      new JobData("JOB_PAYLOAD", 10, "queue", "TOKEN", 1);
  public static String SAMPLE_JOB_DATA_STRING =
      "{\"jobPayload\":\"JOB_PAYLOAD\",\"delaySeconds\":10,\"queue\":\"queue\",\"jobToken\":\"TOKEN\",\"unackedRetries\":0}";
  public static String SAMPLE_JOB_DATA_STRING_WITH_RETRY =
      "{\"jobPayload\":\"JOB_PAYLOAD\",\"delaySeconds\":10,\"queue\":\"queue\",\"jobToken\":\"TOKEN\",\"unackedRetries\":1}";
  public static JobData SAMPLE_JOB_DATA_WITH_4_RETRIES =
      new JobData("JOB_PAYLOAD", 10, "queue", "TOKEN", 4);
  public static String SAMPLE_JOB_DATA_STRING_WITH_4_RETRIES =
      "{\"jobPayload\":\"JOB_PAYLOAD\",\"delaySeconds\":10,\"queue\":\"queue\",\"jobToken\":\"TOKEN\",\"unackedRetries\":4}";
}
