package com.yerbie.core;

import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ZAddParams;

public class JobManager {
  private static final String REDIS_QUEUE_FORMAT_STRING = "queue_%s";

  private final Jedis jedis;

  public JobManager(Jedis jedis) {
    this.jedis = jedis;
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
    Transaction transaction = jedis.multi();
    transaction.zadd(
        String.format(REDIS_QUEUE_FORMAT_STRING, queue),
        delaySeconds,
        jobToken,
        ZAddParams.zAddParams().nx());
    transaction.set(jobToken, jobPayload);
    transaction.exec();
    return jobToken;
  }

  public void deleteJob(String jobToken, String queue) {
    Transaction transaction = jedis.multi();
    transaction.zrem(String.format(REDIS_QUEUE_FORMAT_STRING, queue), jobToken);
    transaction.del(jobToken);
    transaction.exec();
  }
}
