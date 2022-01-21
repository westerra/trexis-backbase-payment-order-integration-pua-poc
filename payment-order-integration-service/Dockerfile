FROM openjdk:11.0-jre

ARG JAR_FILE
COPY ${JAR_FILE} /app/service.jar

ARG SQL_JAR_FILE

RUN mkdir -p /app/config
RUN chown -R 1000:1000 /app/

USER 1000:1000
ADD --chown=1000:1000 . /app/

EXPOSE 8080

WORKDIR /app/
ENTRYPOINT ["java", "-Dloader.path=/app/", "-jar", "service.jar"]
