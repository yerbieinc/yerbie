# Yerbie
Yerbie is a job queue and scheduler backed by Redis. Yerbie is used for:

- Distributing work across threads or machines
- Scheduling code to run sometime in the future
- Retrying failures asynchronously

This repository contains code for the Yerbie server.
The Yerbie server manages job data, queues and job schedules in Redis while exposing an API for clients to interface with Yerbie.

To learn more about Yerbie, go to the [official Yerbie website](https://www.yerbie.dev).

## Yerbie API
The following describes the Yerbie API with sample HTTP requests. Client libraries will send these requests to interact with Yerbie, however client libraries are still responsible for also handing errors, serializing and deserializing job data, as well as implementing polling and running jobs from the Yerbie server.

### Create Job
This creates a job to be scheduled by Yerbie for a certain delay.

Parameters:  
delaySeconds - amount of seconds to delay the job execution for  
jobData - serialized string of job payload  
queue - queue name for which this job should go to.  
jobToken - the client generated job token  

Sample Request
```
  curl -H "Content-Type: application/json" -X POST -d '{"jobData":"JOB_DATA","delaySeconds":5,"queue":"high_priority_queue","jobToken":"token"}' localhost:5865/jobs/schedule 
```

### Reserve Job
This requests a job from Yerbie to indicate that it is being processed by a client. Yerbie will mark this
job as running, and the client must tell Yerbie that it has finished with the job, otherwise Yerbie will enqueue
the job again after a certain amount of time.
`curl -X POST "localhost:5865/jobs/reserve/{queue_name}"`

Sample Request
```
  curl -X POST "localhost:5865/jobs/reserve/high_priority_queue"
```

### Finish Job
This tells Yerbie that the client has finished processing the job. This way Yerbie will not enqueue the job again.
```
  curl -X POST "localhost:5865/jobs/finished?jobToken=9c992592-fecf-4f0c-8b1e-94906f54ec7c"
```

# Releasing
To releae, commit a tag with the format `v{n.n.n}` like, `v1.2.0`, then push. The new image will automatically be built and added to Dockerhub by CI.

# Local Development
See [DEVELOPING.md](DEVELOPING.md).
