# Yerbie Planned Improvements

## Scheduler Sharding
Right now, when a client schedules a job, all of the jobs go into one place in Redis. As more and more jobs get added into the scheduler,
performance will naturally degrade because scanning through the sorted set is O(logn). Therefore, it makes sense to shard the schedulers.
Every time a client requests to add a scheduled job, we can add it to one of n schedulers, that way the effects of scanning through jobs decreases.

That is, we should have a number of schedulers, that we can add or subtract dynamically. Ultimately though, they will all share the same Redis instance
so the bottleneck at the end will become Redis.


## Redis Partitioning
See [this article](https://redis.io/topics/cluster-tutorial).
