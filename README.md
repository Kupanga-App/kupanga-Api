# Kupanga API

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=spring-security&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![MinIO](https://img.shields.io/badge/MinIO-C72E49?style=for-the-badge&logo=minio&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Mockito](https://img.shields.io/badge/Mockito-788BD2?style=for-the-badge&logo=mockito&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)

Bienvenue sur l'API de l'application Kupanga. Ce projet backend est construit avec Spring Boot et suit une architecture modulaire et sécurisée.

## 🏗️ Architecture

L'architecture backend s'appuie sur une structure en couches (Controller, Service, Repository) avec une base de données PostgreSQL et un stockage objet MinIO.

![Architecture Backend](file:///C:/Users/Leclerc/.gemini/antigravity/brain/2ebb9dc4-cd5a-4913-bd35-6b6a4dd59715/uploaded_media_1769283869867.png)
*(Note : L'image ci-dessus représente l'architecture globale. Ce dépôt concerne la partie Backend/API)*

## 🚀 Installation & Démarrage

### Prérequis

*   Git
*   Docker & Docker Compose
*   Java 21 (pour le développement local sans Docker)

### 1. Cloner le dépôt

Via HTTPS :
```bash
git clone https://github.com/Kupanga-App/kupanga-Api.git
```

Ou via SSH :
```bash
git clone git@github.com:Kupanga-App/kupanga-Api.git
```

### 2. Lancer l'environnement (Docker)

Utilisez Docker Compose pour construire l'image et lancer tous les services (API, Base de données, MinIO) en arrière-plan :

```bash
docker compose -f docker-compose-dev.yml up -d
```

## 🛠️ Accès et Configuration

Une fois les conteneurs démarrés, vous pouvez accéder aux différents services :

| Service | URL / Commande | Identifiants / Info |
| :--- | :--- | :--- |
| **API Backend** | `http://localhost:8089/` | Point d'entrée de l'API |
| **Swagger UI** | [Accéder à Swagger](http://localhost:8089/swagger-ui/index.html) | Documentation interactive de l'API |
| **PostgreSQL** | `localhost:5433/kupanga_dev` | **User:** `kupanga`<br>**Fat:** `devpassword` |
| **MinIO Console**| [http://localhost:9001](http://localhost:9001) | **User:** `minioadmin`<br>**Pass:** `minioadmin` |

> **Note :** Pour se connecter à la base de données via un outil externe comme pgAdmin, utilisez le port `5433` exposé par Docker.

## 🧪 Tests et Intégration Continue (CI/CD)

Le projet intègre des tests unitaires et d'intégration via **JUnit 5** et **Mockito**.
L'intégration continue est gérée par **GitHub Actions** pour assurer la qualité du code à chaque push.

```bash
# Lancer les tests manuellement (si Maven est installé)
./mvnw test
```
