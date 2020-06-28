package com.yerbie.core.manager;

import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ZAddParams;

public class JobManager {
  private static final String REDIS_JOB_KEYS = "jobs";

  private final Jedis jedis;

  public JobManager(Jedis jedis) {
    this.jedis = jedis;
  }

  public String createJob(long delaySeconds, String jobData) {
    String jobToken = UUID.randomUUID().toString();
    Transaction transaction = jedis.multi();
    transaction.zadd(REDIS_JOB_KEYS, delaySeconds, jobToken, ZAddParams.zAddParams().nx());
    transaction.set(jobToken, jobData);
    transaction.exec();
    return jobToken;
  }

  public void deleteJob(String jobToken) {
    Transaction transaction = jedis.multi();
    transaction.zrem(REDIS_JOB_KEYS, jobToken);
    transaction.del(jobToken);
    transaction.exec();
  }
}
