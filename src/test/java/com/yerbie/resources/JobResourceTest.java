package com.yerbie.resources;

import com.yerbie.api.ScheduleJobResponse;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

public class JobResourceTest extends TestCase {
  JobResource jobResource;

  @Before
  public void setUp() {
    jobResource = new JobResource();
  }

  @Test
  public void testJobResource() {
    ScheduleJobResponse jobResponse = jobResource.scheduleJob();
    assertEquals("testToken", jobResponse.getJobToken());
  }
}
