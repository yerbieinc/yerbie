package com.yerbie.resources;

import com.codahale.metrics.annotation.Timed;
import com.yerbie.api.FinishJobResponse;
import com.yerbie.api.ReserveJobResponse;
import com.yerbie.api.ScheduleJobRequest;
import com.yerbie.api.ScheduleJobResponse;
import com.yerbie.core.JobManager;
import com.yerbie.core.exception.DuplicateJobException;
import com.yerbie.core.exception.YerbieWebException;
import javax.servlet.http.HttpServletResponse;
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
  @Produces("application/json")
  @Timed
  public ScheduleJobResponse scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    try {
      return new ScheduleJobResponse(
          jobManager.createJob(
              scheduleJobRequest.getDelaySeconds(),
              scheduleJobRequest.getJobData(),
              scheduleJobRequest.getQueue(),
              scheduleJobRequest.getJobToken()));
    } catch (DuplicateJobException ex) {
      throw new YerbieWebException(
          String.format("Duplicate job token found for token %s", ex.getJobToken()),
          HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @POST
  @Path("/reserve")
  @Produces("application/json")
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

  @POST
  @Path("/finished")
  @Produces("application/json")
  @Timed
  public FinishJobResponse markJobAsFinished(@QueryParam("jobToken") String jobToken) {
    if (jobManager.markJobAsComplete(jobToken)) {
      return new FinishJobResponse(jobToken);
    }

    return new FinishJobResponse(null);
  }
}
