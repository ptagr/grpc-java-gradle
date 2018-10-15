FROM gradle:jdk10 as builder

COPY --chown=gradle:gradle . /home/gradle
WORKDIR /home/gradle
RUN gradle assemble

FROM openjdk:10-jre-slim
EXPOSE 50051
COPY --from=builder /home/gradle/build/libs/grpc-java-gradle-server.jar /app/app.jar
WORKDIR /app
CMD java -jar /app/app.jar