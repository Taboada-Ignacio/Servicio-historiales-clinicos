FROM maven:3.9-eclipse-temurin-21-alpine AS compilacion
WORKDIR /aplicacion

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /aplicacion

RUN addgroup -S aplicacion && adduser -S aplicacion -G aplicacion
COPY --from=compilacion --chown=aplicacion:aplicacion /aplicacion/target/*.jar aplicacion.jar

USER aplicacion
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "aplicacion.jar"]
