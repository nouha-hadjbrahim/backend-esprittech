FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ENV SERVER_PORT=8080
ENV JAVA_OPTS=""

RUN addgroup -S spring && adduser -S spring -G spring

COPY target/*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
