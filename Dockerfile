FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/universal/stage /app
EXPOSE 8558 2551 8080
CMD ["bin/fleetstream-cluster"]
