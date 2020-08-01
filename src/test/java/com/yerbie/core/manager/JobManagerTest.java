package com.yerbie.core.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.yerbie.core.JobManager;
import com.yerbie.core.job.JobData;
import com.yerbie.core.job.JobSerializer;
import com.yerbie.stub.StubData;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

@RunWith(MockitoJUnitRunner.class)
public class JobManagerTest {
  @Mock Jedis mockJedis;
  @Mock Transaction mockTransaction;
  @Mock JobSerializer mockJobSerializer;
  @Mock Response<String> mockResponse;
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
    when(mockResponse.get()).thenReturn("JOB_DATA");
    JobData expectedJobData = new JobData("payload", 1, "queue", "jobToken");

    when(mockTransaction.lpop("ready_jobs_queue")).thenReturn(mockResponse);
    when(mockJobSerializer.deserializeJob("JOB_DATA")).thenReturn(expectedJobData);

    assertEquals(expectedJobData, jobManager.reserveJob("queue").get());

    verify(mockJedis).multi();
    verify(mockTransaction).lpop("ready_jobs_queue");
    verify(mockTransaction).sadd("running_jobs_queue", "JOB_DATA");
    verify(mockTransaction).exec();
  }

  // TODO add tests to validate no job and unable to deserialize reserve job.
}
