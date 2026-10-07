# Backend image, used by Vercel Services (see vercel.json) and usable as-is by Render, Fly.io, etc.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# - Vercel sends traffic to port 80 and keeps the /api prefix in the path; hosts that set PORT override it
# - trust the proxy's X-Forwarded-Host/Proto, so a browser POST from the site's own https origin is
#   recognised as same-origin instead of rejected by CORS ("Invalid CORS request")
# - instances scale to zero and multiply under load: keep each one's share of database connections small
ENV PORT=80 \
    SERVER_SERVLET_CONTEXT_PATH=/api \
    SERVER_FORWARD_HEADERS_STRATEGY=framework \
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 80
CMD ["java", "-jar", "app.jar"]
