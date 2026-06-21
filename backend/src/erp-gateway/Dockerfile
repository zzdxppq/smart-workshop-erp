FROM openjdk:17 AS builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:17
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./

ENV TZ=Asia/Shanghai
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Shanghai"
ENV JAVA_OPTS=""
ENV JAVA_ARGS=""
ENV PROFILE=prod
ENV SERVER_PORT=9080

EXPOSE 9080

ENTRYPOINT ["/bin/bash", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher $JAVA_ARGS"]
