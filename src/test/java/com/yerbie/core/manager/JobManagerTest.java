package com.yerbie.core.manager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableSet;
import com.yerbie.core.JobManager;
import com.yerbie.core.exception.DuplicateJobException;
import com.yerbie.core.job.JobData;
import com.yerbie.core.job.JobSerializer;
import com.yerbie.stub.StubData;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

@RunWith(MockitoJUnitRunner.class)
public class JobManagerTest {
  @Mock Jedis mockJedis;
  @Mock JedisPool mockJedisPool;
  @Mock Transaction mockTransaction;
  @Mock JobSerializer mockJobSerializer;
  JobManager jobManager;

  @Before
  public void setUp() {
    jobManager =
        new JobManager(
            mockJedisPool,
            mockJobSerializer,
            Clock.fixed(StubData.AUGUST_ONE_INSTANT, ZoneId.systemDefault()));
    when(mockJedis.multi()).thenReturn(mockTransaction);
    when(mockJedisPool.getResource()).thenReturn(mockJedis);
  }

  @Test
  public void testCreateJob() throws Exception {
    when(mockJobSerializer.serializeJob(any())).thenReturn(StubData.SAMPLE_JOB_DATA_STRING);
    when(mockJobSerializer.serializeJobUnit(any())).thenReturn(StubData.SAMPLE_JOB_UNIT_STRING);
    when(mockJobSerializer.jobDataToJSONNode(StubData.SAMPLE_JOB_DATA_STRING))
        .thenReturn(StubData.JOB_DATA_JSON_NODE);
    when(mockJedis.hexists("delayed_jobs_timestamp", "jobToken")).thenReturn(false);

    jobManager.createJob(10, "JOB_PAYLOAD", "queue", "jobToken");

    verify(mockJedis).multi();
    verify(mockTransaction)
        .zadd(eq("delayed_jobs"), eq(1596318750.0), eq("delayed_jobs_1596318750"), any());
    verify(mockTransaction).rpush("delayed_jobs_1596318750", StubData.SAMPLE_JOB_DATA_STRING);
    verify(mockTransaction)
        .hset("delayed_jobs_timestamp", "jobToken", StubData.SAMPLE_JOB_UNIT_STRING);
  }

  @Test(expected = DuplicateJobException.class)
  public void testCreateJobDuplicate() throws Exception {
    when(mockJedis.hexists("delayed_jobs_timestamp", "jobToken")).thenReturn(true);

    jobManager.createJob(10, "JOB_PAYLOAD", "queue", "jobToken");
  }

  @Test(expected = DuplicateJobException.class)
  public void testCreateJobDuplicateRunning() throws Exception {
    when(mockJedis.hexists("running_jobs_data", "jobToken")).thenReturn(true);

    jobManager.createJob(10, "JOB_PAYLOAD", "queue", "jobToken");
  }

  @Test
  public void testDeleteJob() throws Exception {
    when(mockJedis.hexists("delayed_jobs_timestamp", "jobToken")).thenReturn(true);
    when(mockJedis.hget("delayed_jobs_timestamp", "jobToken"))
        .thenReturn(StubData.SAMPLE_JOB_UNIT_STRING);
    when(mockJobSerializer.deserializeJobUnit(StubData.SAMPLE_JOB_UNIT_STRING))
        .thenReturn(StubData.SAMPLE_JOB_UNIT);
    when(mockJedis.llen(StubData.SAMPLE_JOB_UNIT.getTimestampBucketKey())).thenReturn(0L);

    jobManager.deleteJob("jobToken");

    verify(mockJedis)
        .lrem(
            StubData.SAMPLE_JOB_UNIT.getTimestampBucketKey(),
            0,
            StubData.SAMPLE_JOB_UNIT.getSerializedJobData().toString());
    verify(mockJedis).hdel("delayed_jobs_timestamp", "jobToken");
    verify(mockJedis).watch(StubData.SAMPLE_JOB_UNIT.getTimestampBucketKey());
    verify(mockJedis).multi();
    verify(mockTransaction).del(StubData.SAMPLE_JOB_UNIT.getTimestampBucketKey());
    verify(mockTransaction).zrem("delayed_jobs", StubData.SAMPLE_JOB_UNIT.getTimestampBucketKey());
  }

  @Test
  public void testReserveJobWhenAvailable() throws Exception {
    when(mockJedis.exists("ready_jobs_queue")).thenReturn(true);
    JobData expectedJobData = new JobData("payload", 1, "queue", "jobToken", 0);

    when(mockJedis.lpop("ready_jobs_queue")).thenReturn("JOB_DATA");
    when(mockJobSerializer.deserializeJob("JOB_DATA")).thenReturn(expectedJobData);

    assertEquals(expectedJobData, jobManager.reserveJob("queue").get());

    verify(mockJedis).zadd(eq("running_jobs"), eq(1596318755.0), eq("jobToken"), any());
  }

  @Test
  public void testReserveJobQueueNotExisting() throws Exception {
    when(mockJedis.exists("ready_jobs_queue")).thenReturn(false);

    assertEquals(Optional.empty(), jobManager.reserveJob("queue"));
  }

  @Test
  public void testReserveJobDeserializable() throws Exception {
    when(mockJedis.exists("ready_jobs_queue")).thenReturn(true);
    when(mockJedis.lpop("ready_jobs_queue")).thenReturn("JOB_DATA");
    when(mockJobSerializer.deserializeJob("JOB_DATA")).thenThrow(new IOException("derp!"));

    assertEquals(Optional.empty(), jobManager.reserveJob("queue"));
  }

  @Test
  public void testHandleJobsIsSuccessful() throws Exception {
    when(mockJedis.zrangeByScore("delayed_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(StubData.AUGUST_ONE_DELAYED_ITEM_LIST));
    when(mockJobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING))
        .thenReturn(StubData.SAMPLE_JOB_DATA);
    when(mockJedis.lpop(StubData.AUGUST_ONE_DELAYED_ITEM_LIST))
        .thenReturn(StubData.SAMPLE_JOB_DATA_STRING)
        .thenReturn(StubData.SAMPLE_JOB_DATA_STRING)
        .thenReturn(null);
    when(mockJedis.exists(StubData.AUGUST_ONE_DELAYED_ITEM_LIST)).thenReturn(true);
    when(mockJedis.llen(StubData.AUGUST_ONE_DELAYED_ITEM_LIST)).thenReturn(1L).thenReturn(0L);

    assertTrue(jobManager.handleDueJobsToBeProcessed(10));

    verify(mockJedis, times(2)).watch(StubData.AUGUST_ONE_DELAYED_ITEM_LIST);
    verify(mockJedis, times(2)).rpush("ready_jobs_queue", StubData.SAMPLE_JOB_DATA_STRING);
    verify(mockJedis, times(2))
        .hdel("delayed_jobs_timestamp", StubData.SAMPLE_JOB_DATA.getJobToken());
    verify(mockJedis).multi();
    verify(mockTransaction).del(StubData.AUGUST_ONE_DELAYED_ITEM_LIST);
    verify(mockTransaction).zrem("delayed_jobs", StubData.AUGUST_ONE_DELAYED_ITEM_LIST);
    verify(mockTransaction).exec();
    verify(mockJedis).unwatch();
  }

  @Test
  public void testHandleJobsButNoToken() throws Exception {
    when(mockJedis.zrangeByScore("delayed_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(StubData.AUGUST_ONE_DELAYED_ITEM_LIST));
    when(mockJedis.exists(StubData.AUGUST_ONE_DELAYED_ITEM_LIST)).thenReturn(false);

    assertTrue(jobManager.handleDueJobsToBeProcessed(10));

    verify(mockJedis).zrem("delayed_jobs", StubData.AUGUST_ONE_DELAYED_ITEM_LIST);
  }

  @Test
  public void testMarkJobAsComplete() {
    when(mockJedis.hexists("running_jobs_data", "jobToken")).thenReturn(true);

    jobManager.markJobAsComplete("jobToken");

    verify(mockJedis).multi();
    verify(mockTransaction).hdel("running_jobs_data", "jobToken");
    verify(mockTransaction).zrem("running_jobs", "jobToken");
    verify(mockTransaction).exec();
  }

  @Test
  public void testHandleJobsNotMarkedCompleteNotComplete() throws Exception {
    String jobToken = StubData.SAMPLE_JOB_DATA.getJobToken();
    when(mockJedis.zrangeByScore("running_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(jobToken));
    when(mockJedis.hget("running_jobs_data", jobToken)).thenReturn(StubData.SAMPLE_JOB_DATA_STRING);
    when(mockJobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING))
        .thenReturn(StubData.SAMPLE_JOB_DATA);
    when(mockJobSerializer.serializeJob(StubData.SAMPLE_JOB_DATA_WITH_RETRY))
        .thenReturn(StubData.SAMPLE_JOB_DATA_STRING_WITH_RETRY);

    assertTrue(jobManager.handleJobsNotMarkedAsComplete(10));

    verify(mockJedis).rpush("ready_jobs_queue", StubData.SAMPLE_JOB_DATA_STRING_WITH_RETRY);
    verify(mockJedis).zrem("running_jobs", jobToken);
    verify(mockJedis).hdel("running_jobs_data", jobToken);
  }

  @Test
  public void testDeserializableRemovesBadData() throws Exception {
    String jobToken = StubData.SAMPLE_JOB_DATA.getJobToken();
    when(mockJedis.zrangeByScore("running_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(jobToken));

    when(mockJedis.hget("running_jobs_data", jobToken)).thenReturn(StubData.SAMPLE_JOB_DATA_STRING);
    when(mockJobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING))
        .thenThrow(new IOException("oops!"));

    assertTrue(jobManager.handleJobsNotMarkedAsComplete(10));

    verify(mockJedis).hdel("running_jobs_data", jobToken);
    verify(mockJedis).zrem("running_jobs", jobToken);
  }

  @Test
  public void testHitsMaxRetryDoesntEnqueue() throws Exception {
    String jobToken = StubData.SAMPLE_JOB_DATA.getJobToken();
    when(mockJedis.zrangeByScore("running_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(jobToken));
    when(mockJedis.hget("running_jobs_data", jobToken))
        .thenReturn(StubData.SAMPLE_JOB_DATA_STRING_WITH_4_RETRIES);
    when(mockJobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING_WITH_4_RETRIES))
        .thenReturn(StubData.SAMPLE_JOB_DATA_WITH_4_RETRIES);

    assertFalse(jobManager.handleJobsNotMarkedAsComplete(10));

    verify(mockJedis).hdel("running_jobs_data", jobToken);
    verify(mockJedis).zrem("running_jobs", jobToken);
  }
}
