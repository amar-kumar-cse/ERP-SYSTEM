# ==============================================================================
# Shiksha ERP — Multi-stage Production Dockerfile
# ==============================================================================

# Stage 1: Build the Application
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimalist, Secure Runtime Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create unprivileged user for security
RUN groupadd -r shiksha && useradd -r -g shiksha -m -d /app shiksha

# Copy built artifact from builder
COPY --from=builder /workspace/target/*.jar app.jar

# Create uploads directory and grant permissions
RUN mkdir -p /app/uploads /app/data && chown -R shiksha:shiksha /app

USER shiksha

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
