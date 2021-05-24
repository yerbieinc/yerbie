# Yerbie
Yerbie is a job queue and scheduler backed by Redis. Yerbie is used for:

- Distributing work across threads or machines
- Scheduling code to run sometime in the future
- Retrying failures asynchronously

... and more! To learn more about Yerbie, go to the [official Yerbie website](https://www.yerbie.dev).

This repository contains code for the Yerbie server.
The Yerbie server manages job data, queues and job schedules in Redis while exposing an API for clients to interface with Yerbie.


## Yerbie API
The following describes the Yerbie API with sample HTTP requests. Client libraries will send these requests to interact with Yerbie, however client libraries are still responsible for also handing errors, serializing and deserializing job data, as well as implementing polling and running jobs from the Yerbie server.

### Create Job
This creates a job to be scheduled by Yerbie with a certain delay to a specific queue.

Body JSON Parameters:
- *delaySeconds* - amount of seconds to delay the job before it becomes available in the queue
- *jobData* - serialized job payload string.
- *queue* - queue name for which this job should go to.
- *jobToken* - the client generated job token.

```
curl -H "Content-Type: application/json" -X POST -d '{JSON_BODY}' localhost:5865/jobs/schedule
```

**Sample Request**

```
  curl -H "Content-Type: application/json" -X POST -d '{"jobData":"JOB_DATA","delaySeconds":5,"queue":"high_priority_queue","jobToken":"token"}' localhost:5865/jobs/schedule 
```

**Sample Response**
```
  {"jobToken":"token","queue":"high_priority_queue","jobData":"JOB_DATA","delaySeconds":5}
```

### Reserve Job
This requests a job from Yerbie to indicate that it is being processed by a client. Yerbie will mark this
job as running, and the client must tell Yerbie that it has finished with the job, otherwise Yerbie will enqueue
the job again after 10 seconds.

```
curl -X POST "localhost:5865/jobs/reserve/{queue_name}"
```

**Sample Request**

```
  curl -X POST "localhost:5865/jobs/reserve/high_priority_queue"
```

**Sample Response**

```
  {"delaySeconds":5,"jobData":"JOB_DATA","queue":"high_priority_queue","jobToken":"token"}
```

### Finish Job
This tells Yerbie that the client has finished processing the job. Yerbie will not enqueue the job again.

```
  curl -X POST "localhost:5865/jobs/finished/{token}"
```

**Sample Request**

```
  curl -X POST "localhost:5865/jobs/finished/token"
```

**Sample Response**

```
  {"jobToken":"token"}
```

### Delete Job
This will delete a job that hasn't already been put onto a queue. If the job is already on a queue, it will **not** be deleted.

```
  curl -X POST "localhost:5865/jobs/delete/{token}"
```

**Sample Request**

```
  curl -X POST "localhost:5865/jobs/delete/token"
```

**Sample Response**

```
  {"jobToken":"token"}
```

# Releasing
To releae, commit a tag with the format `v{n.n.n}` like, `v1.2.0`, then push. The new image will automatically be built and added to Dockerhub by CI.

# Local Development
See [DEVELOPING.md](DEVELOPING.md).
