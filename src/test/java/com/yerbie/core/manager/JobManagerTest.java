package com.yerbie.core.manager;

import static org.mockito.Mockito.*;

import com.yerbie.core.JobManager;
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
  JobManager jobManager;

  @Before
  public void setUp() {
    jobManager = new JobManager(mockJedis);
    when(mockJedis.multi()).thenReturn(mockTransaction);
  }

  @Test
  public void testCreateJob() {
    jobManager.createJob(1000, "jobData", "normal");
    verify(mockJedis).multi();
    verify(mockTransaction).zadd(eq("queue_normal"), eq(1000.0), anyString(), any());
    verify(mockTransaction).set(anyString(), eq("jobData"));
    verify(mockTransaction).exec();
  }

  @Test
  public void testDeleteJob() {
    jobManager.deleteJob("jobToken", "normal");
    verify(mockJedis).multi();
    verify(mockTransaction).zrem("queue_normal", "jobToken");
    verify(mockTransaction).del("jobToken");
    verify(mockTransaction).exec();
  }
}