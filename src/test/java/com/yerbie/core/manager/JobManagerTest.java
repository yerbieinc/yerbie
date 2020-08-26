package com.yerbie.core.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.yerbie.core.JobManager;
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

    jobManager.createJob(10, "JOB_PAYLOAD", "queue");

    verify(mockJedis).multi();
    verify(mockTransaction).zadd(eq("delayed_jobs"), eq(1596318750.0), anyString(), any());
    verify(mockTransaction)
        .hset(eq("delayed_jobs_data"), anyString(), eq(StubData.SAMPLE_JOB_DATA_STRING));
  }

  @Test
  public void testDeleteJob() {
    jobManager.deleteJob("jobToken", "normal");
    verify(mockJedis).multi();
    verify(mockTransaction).zrem("delayed_jobs", "jobToken");
    verify(mockTransaction).hdel("delayed_jobs_data", "jobToken");
    verify(mockTransaction).exec();
  }

  @Test
  public void testReserveJobWhenAvailable() throws Exception {
    when(mockJedis.exists("ready_jobs_queue")).thenReturn(true);
    JobData expectedJobData = new JobData("payload", 1, "queue", "jobToken");

    when(mockJedis.lrange("ready_jobs_queue", 0, 0)).thenReturn(ImmutableList.of("JOB_DATA"));
    when(mockJobSerializer.deserializeJob("JOB_DATA")).thenReturn(expectedJobData);

    assertEquals(expectedJobData, jobManager.reserveJob("queue").get());

    verify(mockJedis).multi();
    verify(mockTransaction).lpop("ready_jobs_queue");
    verify(mockTransaction).zadd(eq("running_jobs"), eq(1596318755.0), eq("jobToken"), any());
    verify(mockTransaction).exec();
  }

  @Test
  public void testReserveJobQueueNotExisting() throws Exception {
    when(mockJedis.exists("ready_jobs_queue")).thenReturn(false);

    assertEquals(Optional.empty(), jobManager.reserveJob("queue"));
  }

  @Test
  public void testReserveJobDeserializable() throws Exception {
    when(mockJedis.exists("ready_jobs_queue")).thenReturn(true);

    when(mockJedis.lrange("ready_jobs_queue", 0, 0)).thenReturn(ImmutableList.of("JOB_DATA"));

    when(mockJobSerializer.deserializeJob("JOB_DATA")).thenThrow(new IOException("derp!"));

    assertEquals(Optional.empty(), jobManager.reserveJob("queue"));

    verify(mockJedis).multi();
    verify(mockTransaction).lpop("ready_jobs_queue");
    verify(mockTransaction).exec();
  }

  @Test
  public void testHandleJobsIsSuccessful() throws Exception {
    when(mockJedis.zrangeByScore("delayed_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(StubData.SAMPLE_JOB_DATA.getJobToken()));
    when(mockJobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING))
        .thenReturn(StubData.SAMPLE_JOB_DATA);
    when(mockJedis.hget("delayed_jobs_data", StubData.SAMPLE_JOB_DATA.getJobToken()))
        .thenReturn(StubData.SAMPLE_JOB_DATA_STRING);
    when(mockJedis.hexists("delayed_jobs_data", StubData.SAMPLE_JOB_DATA.getJobToken()))
        .thenReturn(true);

    jobManager.handleDueJobsToBeProcessed(10);

    verify(mockJedis).multi();
    verify(mockTransaction).rpush("ready_jobs_queue", StubData.SAMPLE_JOB_DATA_STRING);
    verify(mockTransaction).zrem("delayed_jobs", StubData.SAMPLE_JOB_DATA.getJobToken());
    verify(mockTransaction).exec();
  }

  @Test
  public void testHandleJobsButNoToken() throws Exception {
    when(mockJedis.zrangeByScore("delayed_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(StubData.SAMPLE_JOB_DATA.getJobToken()));
    when(mockJedis.hexists("delayed_jobs_data", StubData.SAMPLE_JOB_DATA.getJobToken()))
        .thenReturn(false);

    jobManager.handleDueJobsToBeProcessed(10);

    verify(mockJedis).zrem("delayed_jobs", StubData.SAMPLE_JOB_DATA.getJobToken());
  }

  @Test
  public void testMarkJobAsComplete() {
    jobManager.markJobAsComplete("jobToken");
    verify(mockJedis).sadd("completed_jobs", "jobToken");
  }

  @Test
  public void testHandleJobsNotMarkedAsCompleteJobComplete() {
    String jobToken = StubData.SAMPLE_JOB_DATA.getJobToken();
    when(mockJedis.zrangeByScore("running_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(jobToken));
    when(mockJedis.sismember("completed_jobs", jobToken)).thenReturn(true);

    assertTrue(jobManager.handleJobsNotMarkedAsComplete(10));

    verify(mockJedis).multi();
    verify(mockJedis).watch("completed_jobs");
    verify(mockTransaction).srem("completed_jobs", jobToken);
    verify(mockTransaction).hdel("running_jobs_data", jobToken);
    verify(mockTransaction).zrem("running_jobs", jobToken);
    verify(mockTransaction).exec();
  }

  @Test
  public void testHandleJobsNotMarkedCompleteNotComplete() throws Exception {
    String jobToken = StubData.SAMPLE_JOB_DATA.getJobToken();
    when(mockJedis.zrangeByScore("running_jobs", 0, 10, 0, 1))
        .thenReturn(ImmutableSet.of(jobToken));
    when(mockJedis.sismember("completed_jobs", jobToken)).thenReturn(false);
    when(mockJedis.hget("running_jobs_data", jobToken)).thenReturn(StubData.SAMPLE_JOB_DATA_STRING);
    when(mockJobSerializer.deserializeJob(StubData.SAMPLE_JOB_DATA_STRING))
        .thenReturn(StubData.SAMPLE_JOB_DATA);

    assertTrue(jobManager.handleJobsNotMarkedAsComplete(10));

    verify(mockJedis).multi();
    verify(mockTransaction).rpush("ready_jobs_queue", StubData.SAMPLE_JOB_DATA_STRING);
    verify(mockTransaction).zrem("running_jobs", jobToken);
    verify(mockTransaction).hdel("running_jobs_data", jobToken);
    verify(mockTransaction).exec();
  }
}
