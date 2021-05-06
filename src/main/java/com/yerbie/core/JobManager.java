package com.yerbie.core;

import com.yerbie.core.exception.DuplicateJobException;
import com.yerbie.core.exception.SerializationException;
import com.yerbie.core.job.JobData;
import com.yerbie.core.job.JobSerializer;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ZAddParams;

public class JobManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(JobManager.class);
  private static final String REDIS_DELAYED_JOBS_SORTED_SET = "delayed_jobs";
  private static final String REDIS_DELAYED_JOBS_DATA_SET = "delayed_jobs_data";
  private static final String REDIS_READY_JOBS_FORMAT_STRING = "ready_jobs_%s";
  private static final String REDIS_RUNNING_JOBS_SORTED_SET = "running_jobs";
  private static final String REDIS_RUNNING_JOBS_DATA_SET = "running_jobs_data";

  // TODO: make this configurable.
  private static final int FAILURE_TIMEOUT_SECONDS = 15;
  private static final int UNACKED_RETRIES_MAX = 5;

  private final JedisPool jedisPool;
  private final JobSerializer jobSerializer;
  private final Clock clock;

  public JobManager(JedisPool jedisPool, JobSerializer jobSerializer, Clock clock) {
    this.jedisPool = jedisPool;
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
  public String createJob(long delaySeconds, String jobPayload, String queue, String jobToken)
      throws DuplicateJobException {
    LOGGER.debug(
        "Adding job with token {} with delaySeconds {} into queue {}",
        jobToken,
        delaySeconds,
        queue);

    try (Jedis jedis = jedisPool.getResource()) {
      if (jedis.hexists(REDIS_DELAYED_JOBS_DATA_SET, jobToken)) {
        throw new DuplicateJobException(jobToken);
      }

      if (jedis.hexists(REDIS_RUNNING_JOBS_DATA_SET, jobToken)) {
        throw new DuplicateJobException(jobToken);
      }

      Transaction transaction = jedis.multi();

      try {
        transaction.zadd(
            REDIS_DELAYED_JOBS_SORTED_SET,
            Instant.now(clock).plusSeconds(delaySeconds).getEpochSecond(),
            jobToken,
            ZAddParams.zAddParams().nx());
        transaction.hset(
            REDIS_DELAYED_JOBS_DATA_SET,
            jobToken,
            jobSerializer.serializeJob(new JobData(jobPayload, delaySeconds, queue, jobToken, 0)));
        transaction.exec();
      } catch (IOException ex) {
        LOGGER.error("Unable to serialize job into jobData.", ex);
        transaction.discard();
        throw new SerializationException();
      }

      LOGGER.debug(
          "Added JobToken {} with delaySeconds {} into queue {}", jobToken, delaySeconds, queue);

      return jobToken;
    }
  }

  public void deleteJob(String jobToken, String queue) {
    try (Jedis jedis = jedisPool.getResource()) {
      Transaction transaction = jedis.multi();

      LOGGER.debug("Deleting job with token {} from queue {}", jobToken, queue);

      transaction.zrem(REDIS_DELAYED_JOBS_SORTED_SET, jobToken);
      transaction.hdel(REDIS_DELAYED_JOBS_DATA_SET, jobToken);

      LOGGER.debug("Deleted job with token {} from queue {}", jobToken, queue);

      transaction.exec();
    }
  }

  /**
   * Marks the job as compelte by adding it to the completed set.
   *
   * <p>The failure handler will see that it is complete, and not queue the job for retrying.
   */
  public boolean markJobAsComplete(String jobToken) {
    try (Jedis jedis = jedisPool.getResource()) {
      if (!jedis.hexists(REDIS_RUNNING_JOBS_DATA_SET, jobToken)) {
        return false;
      }

      Transaction transaction = jedis.multi();
      transaction.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
      transaction.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);
      transaction.exec();

      LOGGER.info("Job with token {} is marked as complete.", jobToken);

      return true;
    }
  }

  /**
   * Reserves a job by removing it from the ready jobs set and into the processing jobs set.
   * debugging.
   *
   * @param queue
   */
  public Optional<JobData> reserveJob(String queue) {
    try (Jedis jedis = jedisPool.getResource()) {
      if (!jedis.exists(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue))) {
        return Optional.empty();
      }

      LOGGER.debug("Attempting to reserve job from queue {}", queue);

      Optional<String> serializedJobOptional =
          Optional.ofNullable(jedis.lpop(String.format(REDIS_READY_JOBS_FORMAT_STRING, queue)));

      if (!serializedJobOptional.isPresent()) {
        LOGGER.debug("No jobs in ready job queue {}", queue);
        return Optional.empty();
      }

      Transaction transaction = jedis.multi();
      String serializedJob = serializedJobOptional.get();

      try {
        JobData jobData = jobSerializer.deserializeJob(serializedJob);

        transaction.zadd(
            REDIS_RUNNING_JOBS_SORTED_SET,
            Instant.now(clock).getEpochSecond() + FAILURE_TIMEOUT_SECONDS,
            jobData.getJobToken(),
            ZAddParams.zAddParams().nx());
        transaction.hset(REDIS_RUNNING_JOBS_DATA_SET, jobData.getJobToken(), serializedJob);
        transaction.exec();

        LOGGER.info(
            "Removed job {} from ready job queue {} for job execution.",
            jobData.getJobToken(),
            queue);
        return Optional.of(jobData);
      } catch (IOException ex) {
        LOGGER.error("Failed to deserialize jobData {}, removing bad job data.", serializedJob, ex);
        return Optional.empty();
      }
    }
  }

  /**
   * Scans for jobs that are ready to run and enqueues them into the appropriate queue. This should
   * only be called by the master scheduler process, and not multiple processes, otherwise this may
   * result in jobs being enqueued twice. Do not call this method without having the parent
   * scheduler lock.
   *
   * @return whether a job was processed or not.
   */
  public boolean handleDueJobsToBeProcessed(long epochSecondsMax) {
    LOGGER.info("Scanning jobs to be processed earlier than {}", epochSecondsMax);

    try (Jedis jedis = jedisPool.getResource()) {
      Set<String> applicableJobTokens =
          jedis.zrangeByScore(REDIS_DELAYED_JOBS_SORTED_SET, 0, epochSecondsMax, 0, 1);

      if (applicableJobTokens.isEmpty()) {
        return false;
      }

      String jobToken = applicableJobTokens.stream().findFirst().get();

      if (!jedis.hexists(REDIS_DELAYED_JOBS_DATA_SET, jobToken)) {
        LOGGER.error("No job data found for token {}, removing token.", jobToken);
        jedis.zrem(REDIS_DELAYED_JOBS_SORTED_SET, jobToken);
        return false;
      }

      String serializedJobData = jedis.hget(REDIS_DELAYED_JOBS_DATA_SET, jobToken);

      Transaction transaction = jedis.multi();

      try {
        JobData jobData = jobSerializer.deserializeJob(serializedJobData);

        transaction.rpush(
            String.format(REDIS_READY_JOBS_FORMAT_STRING, jobData.getQueue()), serializedJobData);
        transaction.zrem(REDIS_DELAYED_JOBS_SORTED_SET, jobToken);
        transaction.hdel(REDIS_DELAYED_JOBS_DATA_SET, jobToken);
        transaction.exec();

        LOGGER.info(
            "Moved job with token {} into queue {}", jobData.getJobToken(), jobData.getQueue());

        return true;
      } catch (IOException ex) {
        LOGGER.error(
            "Failed to deserialize jobData {}, removing bad job data.", serializedJobData, ex);
        transaction.zrem(REDIS_DELAYED_JOBS_SORTED_SET, serializedJobData);
        transaction.hdel(REDIS_DELAYED_JOBS_DATA_SET, jobToken);
        transaction.exec();
        return true;
      }
    }
  }

  /**
   * Scans for jobs that have been in the running jobs sorted set for longer than epochSecondsMax,
   * if so, this means that the client has likely encountered a catastrophic failure and was unable
   * to process the job. We then move the job into the ready set again. There's a potential infinite
   * loop, but if the client can pull jobs then they should also be able to mark jobs as complete.
   * Only call this if you have the parent scheduler lock.
   */
  public boolean handleJobsNotMarkedAsComplete(long epochSecondsMax) {
    LOGGER.info("Scanning for jobs not marked as complete at {}", epochSecondsMax);

    try (Jedis jedis = jedisPool.getResource()) {
      Set<String> applicableJobs =
          jedis.zrangeByScore(REDIS_RUNNING_JOBS_SORTED_SET, 0, epochSecondsMax, 0, 1);

      if (applicableJobs.isEmpty()) {
        return false;
      }

      String jobToken = applicableJobs.stream().findFirst().get();
      String serializedJobData = jedis.hget(REDIS_RUNNING_JOBS_DATA_SET, jobToken);

      try {
        JobData jobData = jobSerializer.deserializeJob(serializedJobData);

        if (jobData.getUnackedRetries() + 1 == UNACKED_RETRIES_MAX) {
          LOGGER.info(
              "Job with token {} in queue {} has reached max unacked retries and will be removed.",
              jobData.getJobToken(),
              jobData.getQueue());

          Transaction transaction = jedis.multi();
          transaction.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
          transaction.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);
          transaction.exec();
          return false;
        }

        LOGGER.info(
            "Moving back job with token {} into queue {}. It has now been retried {} times.",
            jobData.getJobToken(),
            jobData.getQueue(),
            jobData.getUnackedRetries() + 1);

        String newSerializedJobData =
            jobSerializer.serializeJob(
                new JobData(
                    jobData.getJobPayload(),
                    jobData.getDelaySeconds(),
                    jobData.getQueue(),
                    jobData.getJobToken(),
                    jobData.getUnackedRetries() + 1));

        Transaction transaction = jedis.multi();
        transaction.rpush(
            String.format(REDIS_READY_JOBS_FORMAT_STRING, jobData.getQueue()),
            newSerializedJobData);
        transaction.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
        transaction.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);
        transaction.exec();

        return true;
      } catch (IOException ex) {
        LOGGER.error(
            "Could not deserialize job from running job set {}, removing job data",
            serializedJobData,
            ex);
        Transaction transaction = jedis.multi();
        transaction.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
        transaction.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);
        transaction.exec();
        return true;
      }
    }
  }
}
