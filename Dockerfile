FROM gradle:5.6.4-jdk12 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:12-alpine
EXPOSE 9412
COPY --from=builder /home/gradle/src/build/libs/*.jar /app/yerie-server.jar
ENTRYPOINT ["java", "-jar", "/app/yerbie-server.jar"]
