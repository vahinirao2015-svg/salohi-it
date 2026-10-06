FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /opt/hrms
COPY --from=build /src/target/hrms.jar /opt/hrms/hrms.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/opt/hrms/hrms.jar"]
