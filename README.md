# Yerbie
Job Queue and Scheduler


## Local Development
In IntelliJ IDEA CE -> Open as Project -> build.gradle

1. Install Docker.
2. Run `docker-compose up`.
3. This will bring up Yerbie and Redis in separate containers.


## Building the Yerbie Image and Running it
1. In directory root run `docker build -t yerbie:test .`
2. run `docker run -d -p 5865:5865 yerbie:test`.
   If you want to see the output in the same console, remove the daemon argument `-d`.
3. Once it's running in the container, you can curl localhost:5865 to hit Yerbie.

This will only build Yerbie, but none of its dependencies.

# Docker Cheatsheet

`docker ps` -> This shows running containers.
`docker images` -> Shows images you've built.
