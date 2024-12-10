FROM openjdk:8-jdk-alpine
COPY target/question-bank-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"] 