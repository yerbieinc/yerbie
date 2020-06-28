package com.yerbie.resources;

import com.codahale.metrics.annotation.Timed;
import com.yerbie.api.ScheduleJobResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class JobResource {

  @POST
  @Timed
  public ScheduleJobResponse scheduleJob() {
    return new ScheduleJobResponse("testToken");
  }
}
