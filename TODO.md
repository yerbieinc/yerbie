# Yerbie Planned Improvements

## Scheduler Sharding
Right now, when a client schedules a job, all of the jobs go into one place in Redis. As more and more jobs get added into the scheduler,
performance will naturally degrade because scanning through the sorted set is O(logn). Therefore, it makes sense to shard the schedulers.
Every time a client requests to add a scheduled job, we can add it to one of n schedulers, that way the effects of scanning through jobs decreases.

That is, we should have a number of schedulers, that we can add or subtract dynamically. Ultimately though, they will all share the same Redis instance
so the bottleneck at the end will become Redis.

## Scheduler Scanning
Right now, we just simply use a sorted set sorted by timestamp in order to figure out the jobs that should be triggered.
This is easy to do and simple to implement. However, if we decide to forego accuracy and instead make it accurate
to the second, then we could bucket all jobs to the nearest second and insert the bucket timestamp.
Then, we scan the timestamps. We then reduce the amount of elements that we scan because each bucket will have all the elements needed to fire at that scanning interval.

If, for example some application queues has a 500 QPS job creation, instead of scanning 500 jobs in our sorted set, we would only once and find the bucket responsible
for that particular second. This is how resqueue scheduler works, it adds the timestamp to the sorted set, and adds all the jobs to a list with the key being that timestamp.
When the timestamp comes, it looks at that list and handles all the items in that list.
