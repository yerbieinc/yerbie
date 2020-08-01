package com.yerbie.resources;

import static org.mockito.Mockito.when;

import com.yerbie.api.ReserveJobResponse;
import com.yerbie.api.ScheduleJobRequest;
import com.yerbie.api.ScheduleJobResponse;
import com.yerbie.core.JobManager;
import com.yerbie.stub.StubData;
import java.util.Optional;
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
    when(jobManager.createJob(100, "jobData", "jobQueue")).thenReturn("testToken");

    ScheduleJobRequest scheduleJobRequest = new ScheduleJobRequest(100, "jobData", "jobQueue");
    ScheduleJobResponse jobResponse = jobResource.scheduleJob(scheduleJobRequest);
    assertEquals("testToken", jobResponse.getJobToken());
  }

  @Test
  public void testReserveJob() {
    ReserveJobResponse expectedReserveJobResponse =
        new ReserveJobResponse(
            StubData.SAMPLE_JOB_DATA.getDelaySeconds(),
            StubData.SAMPLE_JOB_DATA.getJobPayload(),
            StubData.SAMPLE_JOB_DATA.getQueue(),
            StubData.SAMPLE_JOB_DATA.getJobToken());
    when(jobManager.reserveJob("queue")).thenReturn(Optional.of(StubData.SAMPLE_JOB_DATA));

    assertEquals(expectedReserveJobResponse, jobResource.reserveJob("queue"));
  }
}
