package com.yerbie.resources;

import static org.mockito.Mockito.when;

import com.yerbie.api.ScheduleJobRequest;
import com.yerbie.api.ScheduleJobResponse;
import com.yerbie.core.manager.JobManager;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JobResourceTest extends TestCase {

  JobResource jobResource;
  @Mock JobManager jobManager;

  @Before
  public void setUp() {
    jobResource = new JobResource(jobManager);
  }

  @Test
  public void testScheduleJob() {
    when(jobManager.createJob(100, "jobData")).thenReturn("testToken");

    ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest("jobData", 100);
    ScheduleJobResponse jobResponse = jobResource.scheduleJob(scheduleJobRequest);
    assertEquals("testToken", jobResponse.getJobToken());
  }
}
