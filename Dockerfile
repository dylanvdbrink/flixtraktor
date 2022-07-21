FROM seleniarm/standalone-chromium:latest

ARG JAR_FILE

ENV FLIXTRAKTOR_SYNC_CRON 0 45 */1 * * ?
ENV FLIXTRAKTOR_SYNC_PAGESIZE 50
ENV FLIXTRAKTOR_SYNC_ALL_ON_INIT false
ENV FLIXTRAKTOR_NETFLIX_USERNAME ""
ENV FLIXTRAKTOR_NETFLIX_PASSWORD ""
ENV FLIXTRAKTOR_NETFLIX_PROFILE ""
ENV FLIXTRAKTOR_CLIENT_ID ""
ENV FLIXTRAKTOR_CLIENT_SECRET ""
ENV FLIXTRAKTOR_LOGGING_LEVEL INFO
ENV FLIXTRAKTOR_NOTIFICATION_ENABLED false
ENV FLIXTRAKTOR_NOTIFICATION_TYPE ""
ENV FLIXTRAKTOR_NOTIFICATION_WEBHOOK_URL ""

VOLUME /tmp
VOLUME /opt/flixtraktor/configuration
VOLUME /opt/flixtraktor/logs

USER root

RUN apt update \
    && apt install openjdk-17-jdk openjdk-17-jre -y

RUN mkdir -p /opt/flixtraktor/configuration
RUN mkdir -p /opt/flixtraktor/logs

#USER 1001

COPY ${JAR_FILE} /opt/flixtraktor/flixtraktor.jar
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production", "/opt/flixtraktor/flixtraktor.jar"]