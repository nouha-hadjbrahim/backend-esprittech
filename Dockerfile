FROM eclipse-temurin:17-jre-alpine@sha256:02320dd4ce20e243dfb915c686089cf9315c763084fafbb12d5c9993aee18b57

WORKDIR /app

ENV SERVER_PORT=8080
ENV JAVA_OPTS=""

RUN apk upgrade --no-cache \
    && addgroup -S spring \
    && adduser -S spring -G spring

COPY target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
