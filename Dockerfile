# ---- Stage 1: build the jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and pom first, then the source
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# Tests are skipped here: the context-load test needs a live MySQL database,
# which doesn't exist at image-build time. CI runs the full suite on every push.
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

# ---- Stage 2: small runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a non-root user
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Tuned for a 512 MB container (e.g. Render's free tier):
#  - heap capped at 70% of the container's memory, leaving room for the JVM itself
#  - serial garbage collector and C1-only JIT: less memory, faster start-up
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

EXPOSE 8080

# Hosting platforms such as Render tell the app which port to listen on through
# the PORT environment variable; fall back to 8080 when it isn't set.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
