package com.yerbie.core;

import com.yerbie.core.job.JobData;
import com.yerbie.core.job.JobSerializer;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ZAddParams;

public class JobManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(JobManager.class);
  private static final String REDIS_DELAYED_JOBS_SORTED_SET = "delayed_jobs";
  private static final String REDIS_JOB_DATA_SORTED_SET = "job_data";
  private static final String REDIS_READY_JOBS_FORMAT_STRING = "ready_jobs_%s";
  private static final String REDIS_RUNNING_JOBS_FORMAT_STRING = "running_jobs_%s";

  private final Jedis jedis;
  private final JobSerializer jobSerializer;
  private final Clock clock;

  public JobManager(Jedis jedis, JobSerializer jobSerializer, Clock clock) {
    this.jedis = jedis;
    this.jobSerializer = jobSerializer;
    this.clock = clock;
  }

  /**
   * Creates a job by adding job token with a delay in Redis' sorted set and setting the job token
   * to the job data in Redis.
   *
   * <p>Here the job data is explicitly decoupled from the sorted set because the data in the sorted
   * set should only contain metadata about the job and not the job payload itself.
   */
  public String createJob(long delaySeconds, String jobPayload, String queue) {
    String jobToken = UUID.randomUUID().toString();

    LOGGER.debug(
        "Adding job with token {} with delaySeconds {} into queue {}",
        jobToken,
        delaySeconds,
        queue);

    Transaction transaction = jedis.multi();
    transaction.zadd(
        REDIS_DELAYED_JOBS_SORTED_SET,
        Instant.now(clock).plusSeconds(delaySeconds).getEpochSecond(),
        jobToken,
        ZAddParams.zAddParams().nx());
    transaction.sadd(REDIS_JOB_DATA_SORTED_SET, jobToken, jobPayload);
    transaction.exec();

    LOGGER.debug(
        "Added JobToken {} with delaySeconds {} into queue {}", jobToken, delaySeconds, queue);

    return jobToken;
  }

  public void deleteJob(String jobToken, String queue) {
    Transaction transaction = jedis.multi();

    LOGGER.debug("Deleting job with token {} from queue {}", jobToken, queue);

    transaction.zrem(REDIS_DELAYED_JOBS_SORTED_SET, jobToken);
    transaction.srem(REDIS_JOB_DATA_SORTED_SET, jobToken);

    LOGGER.debug("Deleted job with token {} from queue {}", jobToken, queue);

    transaction.exec();
  }

  /**
   * Reserves a job by removing it from the ready jobs set and into the processing jobs set.
   * debugging.
   *
   * @param queue
   */
  public Optional<JobData> reserveJob(String queue) {
    if (!jedis.exists(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue))) {
      return Optional.empty();
    }

    LOGGER.debug("Attempting to reserve job from queue {}", queue);

    jedis.watch(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue));

    Optional<String> serializedJobOptional =
        jedis.lrange(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue), 0, 0).stream()
            .findFirst();

    if (!serializedJobOptional.isPresent()) {
      jedis.unwatch();
      LOGGER.debug("No jobs in ready job queue {}", queue);
      return Optional.empty();
    }

    Transaction transaction = jedis.multi();
    String serializedJob = serializedJobOptional.get();

    try {
      JobData jobData = jobSerializer.deserializeJob(serializedJob);

      transaction.rpush(String.format(REDIS_RUNNING_JOBS_FORMAT_STRING, queue), serializedJob);
      transaction.lpop(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue));
      transaction.exec();

      LOGGER.debug("Removed job {} from ready job queue {}", jobData.getJobToken(), queue);
      return Optional.of(jobData);
    } catch (IOException ex) {
      LOGGER.error("Failed to deserialize jobData {}, removing bad job data.", serializedJob, ex);
      transaction.lpop(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue));
      transaction.exec();
      return Optional.empty();
    }
  }
}
