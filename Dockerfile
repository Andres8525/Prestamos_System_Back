FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/cobro-0.0.1-SNAPSHOT.jar app.jar
# El plan Free de Render tiene 512 MB de RAM: se limita el heap para evitar reinicios por memoria.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
