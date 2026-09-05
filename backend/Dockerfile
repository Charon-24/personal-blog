FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 blog
COPY --from=build /workspace/target/personal-blog-*.jar app.jar
RUN mkdir -p /data/media && chown -R blog:blog /app /data
USER blog
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
