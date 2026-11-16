# Compilación: imagen Debian (no Alpine) porque el compilador protoc que descarga Maven
# para generar los stubs gRPC está enlazado con glibc.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/cobro-0.0.1-SNAPSHOT.jar app.jar
# El plan Free de Render tiene 512 MB de RAM: se limitan heap, metaspace y memoria directa (Netty/gRPC).
# TieredStopAtLevel=1 acorta el arranque, que es lo que se espera cuando Render despierta el servicio.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
# 8080: API REST (puerto público de Render). 9090: gRPC (red privada o local).
EXPOSE 8080 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
