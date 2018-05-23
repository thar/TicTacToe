FROM maven:3-jdk-8-alpine as builder
COPY . /code/
WORKDIR /code
RUN mvn -Dmaven.test.skip=true package

FROM openjdk:8-jre
COPY --from=builder /code/target/*.jar /usr/app/
WORKDIR /usr/app
CMD [ "java", "jar","javawebapp0.0.1.jar" ]
