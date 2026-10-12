# -------------------------
# 1️⃣ Build Stage
# -------------------------
# D7 : versions figées (jamais de tag flottant) ; à mettre à jour volontairement
FROM eclipse-temurin:21.0.8_9-jdk-alpine AS build

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
FROM eclipse-temurin:21.0.8_9-jre-alpine

WORKDIR /app

# D2 : l'application ne tourne pas en root
RUN addgroup -S app && adduser -S -G app app

# Copier uniquement le JAR depuis l'étape build (propriété de root, lecture seule pour `app`)
COPY --from=build /app/target/kupanga-0.0.1-SNAPSHOT.jar app.jar

USER app

# D2 : l'image démarre en profil prod si l'hébergeur oublie la variable (sinon : dev, Swagger public…).
# Le compose de dev la surcharge.
ENV SPRING_PROFILES_ACTIVE=prod

# Exposer le port (Spring Boot écoute sur 8089, variable PORT)
ENV PORT=8089
EXPOSE 8089

# D2 : le tas suit la limite mémoire du conteneur (512 Mo dans le compose) au lieu de la RAM de l'hôte
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# D2 : sonde de vivacité (état interne de l'app seulement) : une panne de Redis ou de la base ne doit pas
# faire redémarrer le conteneur en boucle (Redis est en fail-open). wget est fourni par busybox (alpine).
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null "http://127.0.0.1:${PORT}/actuator/health/liveness" || exit 1

# Commande de lancement (exec : java reçoit SIGTERM et peut s'arrêter proprement)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
