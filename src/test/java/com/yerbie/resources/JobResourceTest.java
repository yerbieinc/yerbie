package com.yerbie.resources;

import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.yerbie.api.ReserveJobResponse;
import com.yerbie.api.ScheduleJobRequest;
import com.yerbie.api.ScheduleJobResponse;
import com.yerbie.core.JobManager;
import com.yerbie.core.exception.DuplicateJobException;
import com.yerbie.core.exception.YerbieWebException;
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
    jobResource = new JobResource(jobManager, new MetricRegistry());
  }

  @Test
  public void testScheduleJob() throws Exception {
    when(jobManager.createJob(100, "jobData", "jobQueue", "jobToken")).thenReturn("testToken");

    ScheduleJobRequest scheduleJobRequest =
        new ScheduleJobRequest(100, "jobData", "jobQueue", "jobToken");
    ScheduleJobResponse jobResponse = jobResource.scheduleJob(scheduleJobRequest);
    assertEquals("testToken", jobResponse.getJobToken());
  }

  @Test(expected = YerbieWebException.class)
  public void testScheduleJobDuplicate() throws Exception {
    when(jobManager.createJob(100, "jobData", "jobQueue", "jobToken"))
        .thenThrow(new DuplicateJobException("jobToken"));

    ScheduleJobRequest scheduleJobRequest =
        new ScheduleJobRequest(100, "jobData", "jobQueue", "jobToken");
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
