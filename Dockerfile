# ---------- build stage ----------
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom first for layer caching
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

# Copy sources
COPY src src

# Build
RUN ./mvnw -B clean package -DskipTests

# Normalize jar name
RUN JAR=$(ls target/*.jar | head -n 1) && cp "$JAR" /app/app.jar


# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Kolkata
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
