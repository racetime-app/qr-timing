# Container image for Google Cloud Run.
#
# Stage 1 builds the executable jar with Maven, stage 2 runs it on a slim JRE image.
# Build locally with:   docker build -t qr-timing .
# Run locally with:     docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=demo qr-timing

# ---- Build stage -------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Download dependencies first, in their own layer, so code changes don't re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Don't run as root inside the container.
RUN useradd --system --no-create-home qrtiming
USER qrtiming

COPY --from=build /build/target/qr-timing.jar app.jar

# Cloud Run tells the container which port to listen on through $PORT (8080 by default);
# application.yaml reads it.
ENV PORT=8080
EXPOSE 8080

# MaxRAMPercentage lets the JVM size its heap from the memory limit Cloud Run gives the container.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
