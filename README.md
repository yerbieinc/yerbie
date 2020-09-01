# Yerbie
Job Queue and Scheduler

## Yerbie API

### Create Job
This creates a job to be scheduled by Yerbie for a certain delay.

Parameters:
delaySeconds - amount of seconds to delay the job execution for
jobData - serialized string of job payload
queue - queue name for which this job should go to.

Sample Request
```
  curl -H "Content-Type: application/json" -X POST -d '{"jobData":"JOB_DATA","delaySeconds":5,"queue":"high_priority_queue"}' localhost:5865/jobs/schedule 
```

### Reserve Job
This requests a job from Yerbie to indicate that it is being processed by a worker. Yerbie will mark this
job as running, and the worker must tell Yerbie that it has finished with the job, otherwise Yerbie will enqueue
the job again after a certain amount of time.

Sample Request
```
curl -X POST "localhost:5865/jobs/reserve?queue=high_priority_queue"
```

## Local Development
In IntelliJ IDEA CE -> Open as Project -> build.gradle

1. Install Docker.
2. Run `docker-compose up`. This will bring up Yerbie and Redis in separate containers.
3. `curl localhost:5865/admin/healthcheck` to run healthchecks and verify everything is working.

## Connecting to the Redis Container via Redis CLI
1. Run `docker-compose build`.
2. Run `docker-compose up`
3. Run `docker ps` to find the id of the redis container.
4. Run `docker exec -it <container id> redis-cli -h redis` to connect to redis via the redis cli.


## Building the Yerbie Image and Running it
1. In directory root run `docker build -t yerbie:test .`
2. run `docker run -d -p 5865:5865 yerbie:test`.
   If you want to see the output in the same console, remove the daemon argument `-d`.
3. Once it's running in the container, you can curl localhost:5865 to hit Yerbie.

This will only build Yerbie, but none of its dependencies.

# Docker Cheatsheet

`docker ps` -> This shows running containers.

`docker images` -> Shows images you've built.

`docker kill <container_id>` -> Kills a container. Useful for testing distributed locking mechanisms.
