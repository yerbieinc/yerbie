package com.yerbie.core;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main thread which scans for jobs ready to be scheduled and added to a queue ready to
 * be processed by workers.
 *
 * <p>TODO we need locking and failover mechanisms here otherwise we risk enqueueing more than one
 * item.
 */
public class JobScheduler implements Managed {
  private static final long SLEEP_SECONDS = 3;
  private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);

  private final ExecutorService executorService;
  private final JobManager jobManager;
  private final Clock clock;
  private final Locking locking;
  private boolean processing;
  private boolean isParent;
  private boolean wasParent;

  public JobScheduler(
      JobManager jobManager, Clock clock, Locking locking, ExecutorService executorService) {
    this.executorService = executorService;
    this.jobManager = jobManager;
    this.clock = clock;
    this.locking = locking;
    this.processing = false;
    this.isParent = false;
    this.wasParent = false;
  }

  @Override
  public void start() {
    LOGGER.info("Starting scheduler.");

    processing = true;

    executorService.submit(
        () -> {
          while (processing) {
            boolean processedJobs = false;

            try {
              processedJobs = doWork();
            } catch (Exception ex) {
              // TODO only catch intermittent errors;
              LOGGER.error("Encountered exception handling failed jobs.", ex);
            }

            if (!processedJobs) {
              try {
                LOGGER.info(
                    "Found no jobs to be processed, sleeping for {} seconds.", SLEEP_SECONDS);
                Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_SECONDS));
              } catch (InterruptedException ex) {
                LOGGER.error("Scheduler interrupted while sleeping.", ex);
              }
            }
          }
        });
  }

  @VisibleForTesting
  protected boolean doWork() {
    long epochSeconds = Instant.now(clock).getEpochSecond();

    isParent = locking.isParent();

    if (isParent != wasParent) {
      LOGGER.info("Scheduler is parent: {}", isParent);
      wasParent = isParent;
    }

    if (isParent) {
      LOGGER.info("Scanning for due jobs and failures.");

      boolean processedJobs = jobManager.handleDueJobsToBeProcessed(epochSeconds);
      processedJobs = jobManager.handleJobsNotMarkedAsComplete(epochSeconds) || processedJobs;

      return processedJobs;
    }

    return false;
  }

  @Override
  public void stop() {
    LOGGER.info("Shutting down scheduler.");

    processing = false;
    executorService.shutdown();

    try {
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      executorService.shutdownNow();
    }
  }
}
