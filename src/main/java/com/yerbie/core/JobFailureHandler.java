package com.yerbie.core;

import io.dropwizard.lifecycle.Managed;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobFailureHandler implements Managed {

  private static final long SLEEP_SECONDS = 3;
  private static final Logger LOGGER = LoggerFactory.getLogger(JobFailureHandler.class);

  private ExecutorService executorService;
  private JobManager jobManager;
  private boolean processing;
  private Clock clock;

  public JobFailureHandler(JobManager jobManager, Clock clock) {
    this.jobManager = jobManager;
    this.executorService = Executors.newSingleThreadExecutor();
    this.processing = false;
    this.clock = clock;
  }

  @Override
  public void start() {
    LOGGER.info("Starting job failure handler.");

    processing = true;

    executorService.submit(
        () -> {
          while (processing) {
            boolean processedJobs = false;

            try {
              processedJobs =
                  jobManager.handleJobsNotMarkedAsComplete(Instant.now(clock).getEpochSecond());
            } catch (Exception ex) {
              LOGGER.error("Encountered exception handling failed jobs.", ex);
            }

            if (!processedJobs) {
              try {
                LOGGER.info("Found no failed jobs, sleeping for {} seconds", SLEEP_SECONDS);
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
    LOGGER.info("Shutting down job failure handler.");

    processing = false;
    executorService.shutdown();

    try {
      executorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      executorService.shutdownNow();
    }
  }
}
