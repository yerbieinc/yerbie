package com.yerbie.core;

import io.dropwizard.lifecycle.Managed;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

  private ExecutorService executorService;
  private JobManager jobManager;
  private Clock clock;
  private boolean processing;

  public JobScheduler(JobManager jobManager, Clock clock) {
    this.executorService = Executors.newSingleThreadExecutor();
    this.jobManager = jobManager;
    this.clock = clock;
    this.processing = false;
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
              processedJobs =
                  jobManager.handleDueJobsToBeProcessed(Instant.now(clock).getEpochSecond());
            } catch (Exception ex) {
              LOGGER.error("Encountered exception handling failed jobs.", ex);
            }

            if (!processedJobs) {
              try {
                LOGGER.info(
                    "Found no jobs to be processed, sleeping for {} seconds", SLEEP_SECONDS);
                Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_SECONDS));
              } catch (InterruptedException ex) {
                LOGGER.error("Scheduler interrupted while sleeping.", ex);
              }
            }
          }
        });
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
