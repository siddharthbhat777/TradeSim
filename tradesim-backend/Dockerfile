FROM eclipse-temurin:25-jre-alpine-3.23

WORKDIR /app

COPY target/tradesim.jar tradesim.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "tradesim.jar"]