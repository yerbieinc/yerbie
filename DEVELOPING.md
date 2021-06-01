# Development
To develop with Yerbie, you need a local Redis instance to connect to. To easily facilitate this, the [docker-compose.yml](docker-compose.yml) file brings up Yerbie and Redis for easy local development.

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
2. run `docker run -d -p 5865:5865 yerbie:test`.  If you want to see the output in the same console, remove the daemon argument `-d`.
3. Once it's running in the container, you can curl localhost:5865 to hit Yerbie.

This will only build Yerbie, but none of its dependencies.

# Docker Cheatsheet

`docker ps` -> This shows running containers.

`docker images` -> Shows images you've built.

`docker kill <container_id>` -> Kills a container. Useful for testing distributed locking mechanisms.

# Releasing
To release, commit a tag with the format `v{n.n.n}` like, `v1.2.0`, then push. The new image will automatically be built and added to Dockerhub by CI.
