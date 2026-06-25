FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY build/libs/*.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract
RUN mkdir -p meta-file && \
    cp build/resources/main/META-INF/apt-version* ./meta-file/ 2>/dev/null || true

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

EXPOSE 80 8081 9345

WORKDIR /opt/app
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./
COPY --from=builder /app/meta-file/ ./META-INF/

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]