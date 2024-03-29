package dev.yerbie.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import dev.yerbie.stub.StubData;
import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JobSchedulerHandlerTest {
  @Mock Locking mockLocking;
  @Mock JobManager mockJobManager;
  Clock clock = Clock.fixed(StubData.AUGUST_ONE_INSTANT, ZoneId.systemDefault());
  JobSchedulerHandler jobSchedulerHandler;

  @Before
  public void setUp() {
    jobSchedulerHandler =
        new JobSchedulerHandler(
            mockJobManager, clock, mockLocking, Executors.newSingleThreadScheduledExecutor());
  }

  @Test
  public void testDoesntDoWorkIfNotParent() {
    when(mockLocking.isParent()).thenReturn(false);

    assertFalse(jobSchedulerHandler.doWork());

    verify(mockJobManager, never())
        .handleJobsNotMarkedAsComplete(StubData.AUGUST_ONE_INSTANT.getEpochSecond());
    verify(mockJobManager, never())
        .handleDueJobsToBeProcessed(StubData.AUGUST_ONE_INSTANT.getEpochSecond());
  }

  @Test
  public void testDoesWorkIfParent() {
    when(mockLocking.isParent()).thenReturn(true);
    when(mockJobManager.handleDueJobsToBeProcessed(StubData.AUGUST_ONE_INSTANT.getEpochSecond()))
        .thenReturn(false);
    when(mockJobManager.handleJobsNotMarkedAsComplete(StubData.AUGUST_ONE_INSTANT.getEpochSecond()))
        .thenReturn(true);

    assertTrue(jobSchedulerHandler.doWork());

    verify(mockJobManager)
        .handleJobsNotMarkedAsComplete(StubData.AUGUST_ONE_INSTANT.getEpochSecond());
    verify(mockJobManager).handleDueJobsToBeProcessed(StubData.AUGUST_ONE_INSTANT.getEpochSecond());
  }

  @Test
  public void testCallsJobManagerButStillReturnsFalse() {
    when(mockLocking.isParent()).thenReturn(true);
    when(mockJobManager.handleDueJobsToBeProcessed(StubData.AUGUST_ONE_INSTANT.getEpochSecond()))
        .thenReturn(false);
    when(mockJobManager.handleJobsNotMarkedAsComplete(StubData.AUGUST_ONE_INSTANT.getEpochSecond()))
        .thenReturn(false);

    assertFalse(jobSchedulerHandler.doWork());

    verify(mockJobManager)
        .handleJobsNotMarkedAsComplete(StubData.AUGUST_ONE_INSTANT.getEpochSecond());
    verify(mockJobManager).handleDueJobsToBeProcessed(StubData.AUGUST_ONE_INSTANT.getEpochSecond());
  }
}
