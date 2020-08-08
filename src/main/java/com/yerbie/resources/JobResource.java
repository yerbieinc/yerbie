package com.yerbie.resources;

import com.codahale.metrics.annotation.Timed;
import com.yerbie.api.ReserveJobResponse;
import com.yerbie.api.ScheduleJobRequest;
import com.yerbie.api.ScheduleJobResponse;
import com.yerbie.core.JobManager;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class JobResource {

  private static final ReserveJobResponse NO_JOB_RESPONSE =
      new ReserveJobResponse(0, null, null, null);
  private final JobManager jobManager;

  public JobResource(JobManager jobManager) {
    this.jobManager = jobManager;
  }

  @POST
  @Path("/schedule")
  @Timed
  public ScheduleJobResponse scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    return new ScheduleJobResponse(
        jobManager.createJob(
            scheduleJobRequest.getDelaySeconds(),
            scheduleJobRequest.getJobData(),
            scheduleJobRequest.getQueue()));
  }

  @POST
  @Path("/reserve")
  @Timed
  public ReserveJobResponse reserveJob(@QueryParam("queue") String jobQueue) {
    return jobManager
        .reserveJob(jobQueue)
        .map(
            jobData ->
                new ReserveJobResponse(
                    jobData.getDelaySeconds(),
                    jobData.getJobPayload(),
                    jobData.getQueue(),
                    jobData.getJobToken()))
        .orElse(NO_JOB_RESPONSE);
  }
}
