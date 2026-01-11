# -------------------------
# 1️⃣ Build Stage
# -------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

# Définir le répertoire de travail
WORKDIR /app

# Copier le pom et télécharger les dépendances
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copier le code source
COPY src ./src

# Compiler le projet et créer le JAR
RUN ./mvnw clean package -DskipTests

# -------------------------
# 2️⃣ Runtime Stage (l'image finale légère)
# -------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copier uniquement le JAR depuis l'étape build
COPY --from=build /app/target/kupanga-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port (Spring Boot écoute sur 8080)
EXPOSE 8080

# Commande de lancement
ENTRYPOINT ["java","-jar","/app/app.jar"]
