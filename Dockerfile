FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S plum && adduser -S plum -G plum
COPY --from=build /app/build/libs/endorsement-service.jar app.jar
USER plum

# Railway injects PORT at runtime; default to 8080 for local Docker usage
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=json
ENV LOGSTASH_HOST=logstash
EXPOSE ${PORT}

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:${PORT}/actuator/health || exit 1
ENTRYPOINT ["java", \
  "-Dspring.threads.virtual.enabled=true", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
