package com.yerbie.resources;

import com.codahale.metrics.annotation.Timed;
import com.yerbie.api.ScheduleJobRequest;
import com.yerbie.api.ScheduleJobResponse;
import com.yerbie.core.manager.JobManager;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class JobResource {

  private final JobManager jobManager;

  public JobResource(JobManager jobManager) {
    this.jobManager = jobManager;
  }

  @POST
  @Timed
  public ScheduleJobResponse scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    return new ScheduleJobResponse(
        jobManager.createJob(
            scheduleJobRequest.getDelaySeconds(), scheduleJobRequest.getJobData()));
  }
}
