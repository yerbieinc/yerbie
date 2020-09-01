package com.yerbie.core;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RunWith(MockitoJUnitRunner.class)
public class LockingTest {
  @Mock JedisPool mockJedisPool;
  @Mock Jedis mockJedis;
  Locking locking;

  @Before
  public void setUp() {
    when(mockJedisPool.getResource()).thenReturn(mockJedis);
    locking = new Locking(mockJedisPool, "lock_key_value", "acquire_sha", "has_sha");
  }

  @Test
  public void testIsParentAcquired() {
    when(mockJedis.evalsha(
            "acquire_sha",
            ImmutableList.of("yerbie_master_lock_key"),
            ImmutableList.of("lock_key_value")))
        .thenReturn(1L);
    assertTrue(locking.isParent());
  }

  @Test
  public void testIsParentHasLock() {
    when(mockJedis.evalsha(
            "acquire_sha",
            ImmutableList.of("yerbie_master_lock_key"),
            ImmutableList.of("lock_key_value")))
        .thenReturn(0L);
    when(mockJedis.evalsha(
            "has_sha",
            ImmutableList.of("yerbie_master_lock_key"),
            ImmutableList.of("lock_key_value")))
        .thenReturn(1L);
    assertTrue(locking.isParent());
  }

  @Test
  public void testNotAcquiredNotHolding() {
    when(mockJedis.evalsha(
            "acquire_sha",
            ImmutableList.of("yerbie_master_lock_key"),
            ImmutableList.of("lock_key_value")))
        .thenReturn(0L);
    when(mockJedis.evalsha(
            "has_sha",
            ImmutableList.of("yerbie_master_lock_key"),
            ImmutableList.of("lock_key_value")))
        .thenReturn(0L);
    assertFalse(locking.isParent());
  }
}
