# Yerbie
Job queue and scheduler

## Deploy Instructions
1. Install Docker.
2. In directory root run `docker build -t yerbie:test .`
3. run `docker run -d -p 5865:5865 yerbie:test`.
   If you want to see the output in the same console, remove the daemon argument `-d`.


## Local Development
In IntelliJ IDEA CE -> Open as Project -> build.gradle
