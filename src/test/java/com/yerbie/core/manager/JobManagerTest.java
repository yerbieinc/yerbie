package com.yerbie.core.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
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
import redis.clients.jedis.Transaction;

@RunWith(MockitoJUnitRunner.class)
public class JobManagerTest {
  @Mock Jedis mockJedis;
  @Mock Transaction mockTransaction;
  @Mock JobSerializer mockJobSerializer;
  JobManager jobManager;

  @Before
  public void setUp() {
    jobManager =
        new JobManager(
            mockJedis,
            mockJobSerializer,
            Clock.fixed(StubData.AUGUST_ONE_INSTANT, ZoneId.systemDefault()));
    when(mockJedis.multi()).thenReturn(mockTransaction);
  }

  @Test
  public void testCreateJob() {
    jobManager.createJob(1000, "jobData", "normal");
    verify(mockJedis).multi();
    verify(mockTransaction).zadd(eq("delayed_jobs"), eq(1596319740.0), anyString(), any());
    verify(mockTransaction).sadd(eq("job_data"), anyString(), eq("jobData"));
    verify(mockTransaction).exec();
  }

  @Test
  public void testDeleteJob() {
    jobManager.deleteJob("jobToken", "normal");
    verify(mockJedis).multi();
    verify(mockTransaction).zrem("delayed_jobs", "jobToken");
    verify(mockTransaction).srem("job_data", "jobToken");
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
    verify(mockTransaction).zadd(eq("running_jobs"), eq(1596318740.0), eq("JOB_DATA"), any());
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
}
