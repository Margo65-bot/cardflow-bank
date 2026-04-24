FROM amazoncorretto:21-alpine AS builder
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests

FROM amazoncorretto:21-alpine AS layers
WORKDIR /application
COPY --from=builder /app/target/cardflow-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
WORKDIR /application
COPY --from=layers /application/dependencies/ ./
COPY --from=layers /application/spring-boot-loader/ ./
COPY --from=layers /application/snapshot-dependencies/ ./
COPY --from=layers /application/application/ ./

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
