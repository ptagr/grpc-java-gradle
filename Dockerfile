FROM gradle:jdk10 as builder
COPY --chown=gradle:gradle . /home/gradle
WORKDIR /home/gradle
RUN gradle assemble

FROM ubuntu:14.04 as profiler
RUN  apt-get update \
  && apt-get install -y wget \
  && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /opt/cprof && \
    wget -q -O- https://storage.googleapis.com/cloud-profiler/java/latest/profiler_java_agent.tar.gz \
    | tar xzv -C /opt/cprof

FROM openjdk:10-jre-slim
EXPOSE 50051
COPY --from=builder /home/gradle/build/libs/grpc-java-gradle-server.jar /app/app.jar
RUN mkdir -p /opt/cprof
COPY --from=profiler /opt/cprof/profiler_java_agent.so /opt/cprof/profiler_java_agent.so

WORKDIR /app
CMD java -agentpath:/opt/cprof/profiler_java_agent.so=-cprof_service=grpcGradleJava -jar /app/app.jar