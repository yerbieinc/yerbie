FROM gradle:6.3.0-jdk14 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:14-alpine
EXPOSE 5865
COPY --from=builder /home/gradle/src/yerbie.yml /app/yerbie.yml
COPY --from=builder /home/gradle/src/yerbie-server /app/yerbie-server
COPY --from=builder /home/gradle/src/build/libs/*-all.jar /app/yerbie-server.jar

WORKDIR /app

ENTRYPOINT ["/bin/sh", "yerbie-server"]
