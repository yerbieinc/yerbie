package com.yerbie.core;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class Locking {
  private static final Logger LOGGER = LoggerFactory.getLogger(Locking.class);
  private static final String REDIS_PARENT_LOCK_KEY = "yerbie_master_lock_key";

  private final JedisPool jedisPool;
  private final String lockKeyValue;
  private final String acquireScriptSha;
  private final String hasLockScriptSha;

  public Locking(
      JedisPool jedisPool, String lockKeyValue, String acquireScriptSha, String hasLockScriptSha) {
    LOGGER.info(
        "Loading Lua Scripts into Redis. This Yerbie instance has lock value {}", lockKeyValue);

    this.jedisPool = jedisPool;
    this.lockKeyValue = lockKeyValue;
    this.acquireScriptSha = acquireScriptSha;
    this.hasLockScriptSha = hasLockScriptSha;
  }

  /**
   * Attempts to acquire the lock. If it already has the lock, updates the expiry in REDIS to
   * continue being the parent.
   */
  public boolean isParent() {
    return acquireLock() || hasLock();
  }

  private boolean hasLock() {
    return evalScript(hasLockScriptSha);
  }

  private boolean acquireLock() {
    return evalScript(acquireScriptSha);
  }

  private boolean evalScript(String sha) {
    try (Jedis jedis = jedisPool.getResource()) {
      return (long)
              jedis.evalsha(
                  sha, ImmutableList.of(REDIS_PARENT_LOCK_KEY), ImmutableList.of(lockKeyValue))
          == 1;
    }
  }
}
