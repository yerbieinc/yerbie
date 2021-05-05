# Yerbie
Job Queue and Scheduler

## Yerbie API
The following describes the Yerbie API with sample requests with which client libraries interact with.

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

# Deploying
1. Create an eksctl cluster via `eksctl create cluster --name yerbie-cluster --region us-west-2 --ssh-access --ssh-public-key claudioKeyPair --managed`
   If you want to debug what's going on, `eksctl utils describe-stacks --region=us-west-2 --cluster=yerbie-cluster`.
2. Use `kompose` to convert `docker-compose` into Kubernetes orchestrators via `kompose convert`. TODO I should actually write the files myself!
3. Once converted, run `kubectl apply -f redis-deployment.yaml,redis-service.yaml,web-deployment.yaml,web-service.yaml` and check your deployed containers.
4. Once you're done, delete the cluster via `eksctl delete cluster --name yerbie-cluster --region us-west-2`.

# Releasing
1. Commit a tag with the format `v{n.n.n}` like, `v1.2.0`, then push.
2. The new image will automatically be added to Dockerhub by CI.
