package com.yerbie.resources;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.yerbie.api.JobTokenResponse;
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
  private final MetricRegistry metricRegistry;
  private final Timer scheduleJobTimer;
  private final Timer reserveJobTimer;
  private final Timer finishedJobTimer;

  public JobResource(JobManager jobManager, MetricRegistry metricRegistry) {
    this.jobManager = jobManager;
    this.metricRegistry = metricRegistry;
    this.scheduleJobTimer =
        metricRegistry.timer(MetricRegistry.name(JobResource.class, "scheduleJob"));
    this.reserveJobTimer =
        metricRegistry.timer(MetricRegistry.name(JobResource.class, "reserveJob"));
    this.finishedJobTimer =
        metricRegistry.timer(MetricRegistry.name(JobResource.class, "finishedJob"));
  }

  @POST
  @Path("/delete/{jobToken}")
  @Produces("application/json")
  public JobTokenResponse deleteJob(@PathParam("jobToken") String jobToken) {
    try (Timer.Context context = scheduleJobTimer.time()) {
      if (jobManager.deleteJob(jobToken)) {
        return new JobTokenResponse(jobToken);
      }

      return new JobTokenResponse(null);
    }
  }

  @POST
  @Path("/schedule")
  @Produces("application/json")
  public ScheduleJobResponse scheduleJob(ScheduleJobRequest scheduleJobRequest) {
    try (Timer.Context context = scheduleJobTimer.time()) {
      return new ScheduleJobResponse(
          jobManager.createJob(
              scheduleJobRequest.getDelaySeconds(),
              scheduleJobRequest.getJobData(),
              scheduleJobRequest.getQueue(),
              scheduleJobRequest.getJobToken()),
          scheduleJobRequest.getQueue(),
          scheduleJobRequest.getJobData(),
          scheduleJobRequest.getDelaySeconds());
    } catch (DuplicateJobException ex) {
      throw new YerbieWebException(
          String.format("Duplicate job token found for token %s.", ex.getJobToken()),
          HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @POST
  @Path("/reserve/{queue}")
  @Produces("application/json")
  public ReserveJobResponse reserveJob(@PathParam("queue") String jobQueue) {
    try (final Timer.Context context = reserveJobTimer.time()) {
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

  @POST
  @Path("/finished/{jobToken}")
  @Produces("application/json")
  public JobTokenResponse markJobAsFinished(@PathParam("jobToken") String jobToken) {
    try (final Timer.Context context = finishedJobTimer.time()) {
      if (jobManager.markJobAsComplete(jobToken)) {
        return new JobTokenResponse(jobToken);
      }

      return new JobTokenResponse(null);
    }
  }
}
