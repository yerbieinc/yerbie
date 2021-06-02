package dev.yerbie.core;

import dev.yerbie.core.exception.DuplicateJobException;
import dev.yerbie.core.exception.SerializationException;
import dev.yerbie.core.job.JobData;
import dev.yerbie.core.job.JobSerializer;
import dev.yerbie.core.job.JobUnit;
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
  private static final String REDIS_DELAYED_JOBS_TIMESTAMP_SET = "delayed_jobs_timestamp";
  private static final String REDIS_READY_JOBS_FORMAT_STRING = "ready_jobs_%s";
  private static final String REDIS_RUNNING_JOBS_SORTED_SET = "running_jobs";
  private static final String REDIS_RUNNING_JOBS_DATA_SET = "running_jobs_data";
  private static final String DELAYED_ITEMS_LIST = "delayed_jobs_%d";

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
   * Creates a job by adding the timestamp to Redis' sorted set as well as the job data to a list
   * with the key of that timestamp.
   *
   * <p>For example, adding a timestamp of 100 will create 100 in a sorted set, as well as a list
   * `delayed_jobs_100` whose contents are the job data.
   *
   * <p>All jobs due to be executed at time 100 will be located in that list.
   */
  public String createJob(long delaySeconds, String jobPayload, String queue, String jobToken)
      throws DuplicateJobException {
    LOGGER.debug(
        "Adding job with token {} with delaySeconds {} into queue {}",
        jobToken,
        delaySeconds,
        queue);

    try (Jedis jedis = jedisPool.getResource()) {
      if (jedis.hexists(REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobToken)) {
        throw new DuplicateJobException(jobToken);
      }

      if (jedis.hexists(REDIS_RUNNING_JOBS_DATA_SET, jobToken)) {
        throw new DuplicateJobException(jobToken);
      }

      Transaction transaction = jedis.multi();

      Long timestampEpochSeconds = Instant.now(clock).plusSeconds(delaySeconds).getEpochSecond();
      String timestampBucketName = String.format(DELAYED_ITEMS_LIST, timestampEpochSeconds);

      try {
        transaction.zadd(
            REDIS_DELAYED_JOBS_SORTED_SET,
            Instant.now(clock).plusSeconds(delaySeconds).getEpochSecond(),
            timestampBucketName,
            ZAddParams.zAddParams().nx());

        String serializedJob =
            jobSerializer.serializeJob(new JobData(jobPayload, delaySeconds, queue, jobToken, 0));

        transaction.rpush(timestampBucketName, serializedJob);

        JobUnit jobUnit =
            new JobUnit(jobSerializer.jobDataToJSONNode(serializedJob), timestampBucketName);
        transaction.hset(
            REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobToken, jobSerializer.serializeJobUnit(jobUnit));

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

  public boolean deleteJob(String jobToken) {
    try (Jedis jedis = jedisPool.getResource()) {
      if (!jedis.hexists(REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobToken)) {
        return false;
      }

      LOGGER.debug("Deleting job with token {}", jobToken);

      String serializedJobUnit = jedis.hget(REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobToken);

      try {
        JobUnit jobUnit = jobSerializer.deserializeJobUnit(serializedJobUnit);
        String listKey = jobUnit.getTimestampBucketKey();

        jedis.lrem(listKey, 0, jobUnit.getSerializedJobData().toString());
        jedis.hdel(REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobToken);

        cleanupListIfNeeded(listKey, jedis);

        LOGGER.debug("Deleted job with token {}", jobToken);
      } catch (IOException ex) {
        LOGGER.error("Failed to deserialize jobUnit.", ex);
        jedis.hdel(REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobToken);
      }

      return true;
    }
  }

  /**
   * Marks the job as complete.
   *
   * <p>This way, the failure handler will no longer pick it up from the running jobs sorted set for
   * re execution.
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

      String serializedJob = serializedJobOptional.get();

      try {
        JobData jobData = jobSerializer.deserializeJob(serializedJob);

        jedis.zadd(
            REDIS_RUNNING_JOBS_SORTED_SET,
            Instant.now(clock).getEpochSecond() + FAILURE_TIMEOUT_SECONDS,
            jobData.getJobToken(),
            ZAddParams.zAddParams().nx());
        jedis.hset(REDIS_RUNNING_JOBS_DATA_SET, jobData.getJobToken(), serializedJob);

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

  private void cleanupListIfNeeded(String listKey, Jedis jedis) {
    jedis.watch(listKey);

    if (jedis.llen(listKey) == 0) {
      Transaction transaction = jedis.multi();
      transaction.del(listKey);
      transaction.zrem(REDIS_DELAYED_JOBS_SORTED_SET, listKey);
      transaction.exec();
    } else {
      jedis.unwatch();
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
      Set<String> listsToProcess =
          jedis.zrangeByScore(REDIS_DELAYED_JOBS_SORTED_SET, 0, epochSecondsMax, 0, 1);

      if (listsToProcess.isEmpty()) {
        return false;
      }

      String redisTimestampList = listsToProcess.stream().findFirst().get();

      if (!jedis.exists(redisTimestampList)) {
        LOGGER.error("No list found for timestamp {}, removing token.", redisTimestampList);
        jedis.zrem(REDIS_DELAYED_JOBS_SORTED_SET, redisTimestampList);
        return true;
      }

      while (true) {
        Optional<String> serializedJobData = Optional.ofNullable(jedis.lpop(redisTimestampList));

        if (!serializedJobData.isPresent()) {
          break;
        }

        try {
          JobData jobData = jobSerializer.deserializeJob(serializedJobData.get());

          jedis.rpush(
              String.format(REDIS_READY_JOBS_FORMAT_STRING, jobData.getQueue()),
              serializedJobData.get());
          jedis.hdel(REDIS_DELAYED_JOBS_TIMESTAMP_SET, jobData.getJobToken());

          LOGGER.info(
              "Moved job with token {} into queue {}", jobData.getJobToken(), jobData.getQueue());

          cleanupListIfNeeded(redisTimestampList, jedis);
        } catch (IOException ex) {
          LOGGER.error(
              "Failed to deserialize jobData {}, removed bad job data.", serializedJobData, ex);
        }
      }

      return true;
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

          jedis.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
          jedis.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);
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

        jedis.rpush(
            String.format(REDIS_READY_JOBS_FORMAT_STRING, jobData.getQueue()),
            newSerializedJobData);
        jedis.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
        jedis.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);

        return true;
      } catch (IOException ex) {
        LOGGER.error(
            "Could not deserialize job from running job set {}, removing job data",
            serializedJobData,
            ex);
        jedis.hdel(REDIS_RUNNING_JOBS_DATA_SET, jobToken);
        jedis.zrem(REDIS_RUNNING_JOBS_SORTED_SET, jobToken);
        return true;
      }
    }
  }
}
