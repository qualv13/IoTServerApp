# Etap 1: Budowanie aplikacji (Maven)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Budujemy plik .jar, pomijając testy (szybciej)
RUN mvn clean package -Dmaven.test.skip=true

# Etap 2: Uruchamianie aplikacji (Lekki obraz JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Kopiujemy zbudowany plik .jar z poprzedniego etapu
COPY --from=build /app/target/*.jar app.jar

# Wystawiamy port aplikacji (musi się zgadzać z server.port, domyślnie 8080)
EXPOSE 8080

# Komenda startowa
ENTRYPOINT ["java", "-jar", "app.jar"]