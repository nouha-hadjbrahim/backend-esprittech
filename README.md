# EspritTECH & RDI — Module Authentification & Gestion des utilisateurs

Socle d'authentification de la plateforme de gouvernance des projets académiques
(Stage / PFE / RDI). Inscription self-service validée contre un **référentiel CSV**,
JWT (access + refresh) et gestion des **équipes de recherche** (chef désigné par l'admin).

## Stack

- Java 17, Spring Boot **3.4.1**, Spring Security 6
- Spring Data JPA + **MySQL**
- JWT via **jjwt 0.12.x** (access + refresh stateless)
- Secrets dans `.env` (chargé par **spring-dotenv**) ou variables d'environnement — jamais en dur dans Java
- Validation Jakarta, BCrypt, Lombok
- Quality test - SonarQube
- Documentation **OpenAPI / Swagger UI** (springdoc)
- Import CSV via parsing natif (java.io, séparateur virgule, UTF-8)

## Architecture (package `tn.esprit.backend_esprittech`)

```
config/      SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider,
             JwtProperties, CorsConfig, CustomUserDetailsService, DataInitializer
entity/      User, ReferentielUtilisateur, EquipeRecherche
enums/       Role, TypeUtilisateur
repository/  *Repository (Spring Data JPA)
dto/         request/ + response/
service/     AuthService, UserService, ReferentielService, EquipeRechercheService
controller/  AuthController, AdminController, EquipeController
exception/   GlobalExceptionHandler + exceptions métier
mapper/      UserMapper, EquipeMapper
```

## Démarrage

### 1. Pré-requis
- JDK 17+, Maven (ou le wrapper `./mvnw`)
- MySQL en local (port 3306)

### 2. Configuration
```bash
cp .env.example .env
# Éditez .env : JWT_SECRET (>= 32 octets), expirations, identifiants MySQL...
```
`application.properties` mappe `JWT_SECRET` vers `app.jwt.secret`,
`JWT_ACCESS_EXPIRATION` vers `app.jwt.expiration-ms` et `JWT_REFRESH_EXPIRATION`
vers `app.jwt.refresh-expiration-ms`. Le vrai `.env` est ignoré par Git ;
ne committez que `.env.example`.

Windows PowerShell, sans fichier `.env` :

```powershell
$env:JWT_SECRET = "change-this-to-a-secure-random-secret-at-least-32-characters"
$env:JWT_ACCESS_EXPIRATION = "900000"
$env:JWT_REFRESH_EXPIRATION = "604800000"
```

La base est créée automatiquement (`createDatabaseIfNotExist=true`) et le schéma
généré par Hibernate (`ddl-auto=update`).

### SonarQube
See [SONAR_SETUP.md](./SONAR_SETUP.md) for local SonarQube startup, token creation,
and GitLab CI/CD variable setup.

### 3. Lancer
```bash
./mvnw clean spring-boot:run        # Windows : mvnw.cmd clean spring-boot:run
```
Au premier démarrage, un **compte admin** est créé :
`ADMIN_EMAIL` / `ADMIN_PASSWORD` (défaut `admin@esprittech.tn` / `Admin@1234`).

### 4. Documentation API
- Swagger UI : http://localhost:8080/swagger-ui.html
- OpenAPI JSON : http://localhost:8080/v3/api-docs

## Flux d'utilisation

1. **Admin** se connecte (`POST /api/auth/login`) puis importe le référentiel :
   `POST /api/admin/import-referentiel` (multipart, champ `file`).
   Un exemple est fourni : `src/main/resources/sample-referentiel.csv`.
   Format : `nom,prenom,email,identifiant,type,telephone`.
2. **Utilisateur** s'inscrit (`POST /api/auth/register`) — accepté seulement si
   l'email correspond à une ligne du référentiel.
   Le rôle (ETUDIANT/ENSEIGNANT) est déduit du type référentiel.
3. **Admin** crée une équipe (`POST /api/admin/equipes`) en désignant un chef
   enseignant → promu `ROLE_CHEF_EQUIPE`. Réassignation : `PUT /api/admin/equipes/{id}/chef`.
4. **Consultation** des équipes et de leurs membres par tout utilisateur authentifié
   (`GET /api/equipes`, `/api/equipes/{id}`, `/api/equipes/{id}/membres`).

## Endpoints principaux

| Méthode | URL | Accès | Rôle |
|--------|-----|-------|------|
| POST | `/api/auth/register` | public | — |
| POST | `/api/auth/login` | public | — |
| POST | `/api/auth/refresh` | public | — |
| GET  | `/api/auth/me` | authentifié | tous |
| POST | `/api/admin/import-referentiel` | sécurisé | ADMIN |
| POST | `/api/admin/equipes` | sécurisé | ADMIN |
| PUT  | `/api/admin/equipes/{id}/chef` | sécurisé | ADMIN |
| GET  | `/api/equipes` `/api/equipes/{id}` `/api/equipes/{id}/membres` | authentifié | tous |

## Exemple cURL

```bash
# Connexion admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@esprittech.tn","password":"Admin@1234"}'

# Import du référentiel (remplacer <TOKEN>)
curl -X POST http://localhost:8080/api/admin/import-referentiel \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@src/main/resources/sample-referentiel.csv"

# Inscription d'un utilisateur présent dans le référentiel
# (email en @esprit.tn ; mot de passe : 1 maj, 1 min, 1 chiffre, >= 8 car.)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nom":"Hadjbrahim","prenom":"Nouha","email":"nouha.hadjbrahim@esprit.tn","password":"Passw0rd1"}'
```

## Sécurité — notes

- API **stateless** : pas de session, authentification par JWT à chaque requête.
- Le `JwtAuthenticationFilter` lit `Authorization: Bearer <token>`, valide la
  signature/expiration et rejette les refresh tokens pour l'accès aux ressources.
- Le refresh token est un JWT signé portant le claim `type=refresh` (stratégie
  stateless ; pour une révocation fine, basculer vers une table dédiée).
- `JWT_SECRET` doit être non vide et faire **au moins 32 octets** pour HS256.
  Si la valeur manque ou est trop courte, l'application refuse de démarrer avec
  un message indiquant de définir `JWT_SECRET` dans l'environnement ou `.env`.
- En production : remplacer `ddl-auto=update` par une migration (Flyway/Liquibase),
  changer le mot de passe admin, restreindre les origines CORS.
