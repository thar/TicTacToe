FROM openjdk:8-jdk-alpine

ARG GIT_COMMIT=unspecified
LABEL git_commit=$GIT_COMMIT

ARG COMMIT_DATE=unspecified
LABEL commit_date=$COMMIT_DATE

COPY target/*.jar /usr/app/app.jar
WORKDIR /us/app
ENTRYPOINT ["java", "-jar", "/usr/app/app.jar"]

