# EspritTECH & RDI — Module Authentification & Gestion des utilisateurs

Socle d'authentification de la plateforme de gouvernance des projets académiques
(Stage / PFE / RDI). Inscription self-service validée contre un **référentiel CSV**,
JWT (access + refresh), gestion des **équipes de recherche** et du workflow
d'**affiliation** des enseignants.

## Stack

- Java 17, Spring Boot **3.4.1**, Spring Security 6
- Spring Data JPA + **MySQL**
- JWT via **jjwt 0.12.x** (access + refresh stateless)
- Secrets dans `.env` (chargé par **spring-dotenv**) — jamais dans `application.yml`
- Validation Jakarta, BCrypt, Lombok
- Documentation **OpenAPI / Swagger UI** (springdoc)
- Import CSV via Apache Commons CSV

## Architecture (package `tn.esprit.backend_esprittech`)

```
config/      SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider,
             DotenvConfig, CorsConfig, CustomUserDetailsService, DataInitializer
entity/      User, ReferentielUtilisateur, EquipeRecherche, AffiliationEnseignant
enums/       Role, TypeUtilisateur, StatutAffiliation
repository/  *Repository (Spring Data JPA)
dto/         request/ + response/
service/     AuthService, UserService, ReferentielService,
             EquipeRechercheService, AffiliationService
controller/  AuthController, AdminController, EquipeController, AffiliationController
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
# Éditez .env : JWT_SECRET (>= 32 caractères !), identifiants MySQL...
```
La base est créée automatiquement (`createDatabaseIfNotExist=true`) et le schéma
généré par Hibernate (`ddl-auto=update`).

### 3. Lancer
```bash
./mvnw spring-boot:run        # Windows : mvnw.cmd spring-boot:run
```
Au premier démarrage, un **compte admin** est créé :
`ADMIN_EMAIL` / `ADMIN_PASSWORD` (défaut `admin@esprittech.tn` / `Admin@1234`).

### 4. Documentation API
- Swagger UI : http://localhost:8080/swagger-ui.html
- OpenAPI JSON : http://localhost:8080/v3/api-docs

## Flux d'utilisation

1. **Admin** se connecte (`POST /api/auth/login`) puis importe le référentiel :
   `POST /api/admin/referentiel/import` (multipart, champ `file`).
   Un exemple est fourni : `src/main/resources/sample-referentiel.csv`.
   Format : `nom,prenom,email,identifiant,type,departement`.
2. **Utilisateur** s'inscrit (`POST /api/auth/register`) — accepté seulement si
   email **et** identifiant correspondent à la même ligne du référentiel.
   Le rôle (ETUDIANT/ENSEIGNANT) est déduit du type référentiel.
3. **Admin** crée une équipe (`POST /api/equipes`) en désignant un chef enseignant
   → promu `ROLE_CHEF_EQUIPE`.
4. **Enseignant** demande son affiliation (`POST /api/affiliations/demande`).
5. **Chef d'équipe** accepte/refuse (`PATCH /api/affiliations/{id}/accepter|refuser`).
6. **Vérification** d'affiliation (consommée par le futur module Sujets) :
   `GET /api/affiliations/verifier/{enseignantId}`.

## Endpoints principaux

| Méthode | URL | Accès | Rôle |
|--------|-----|-------|------|
| POST | `/api/auth/register` | public | — |
| POST | `/api/auth/login` | public | — |
| POST | `/api/auth/refresh` | public | — |
| GET  | `/api/auth/me` | authentifié | tous |
| POST | `/api/admin/referentiel/import` | sécurisé | ADMIN |
| POST | `/api/equipes` | sécurisé | ADMIN |
| GET  | `/api/equipes` `/api/equipes/{id}` | authentifié | tous |
| POST | `/api/affiliations/demande` | sécurisé | ENSEIGNANT / CHEF_EQUIPE |
| PATCH| `/api/affiliations/{id}/accepter` | sécurisé | CHEF_EQUIPE |
| PATCH| `/api/affiliations/{id}/refuser` | sécurisé | CHEF_EQUIPE |
| GET  | `/api/affiliations/equipe/{equipeId}` | sécurisé | CHEF_EQUIPE |
| GET  | `/api/affiliations/verifier/{enseignantId}` | authentifié | tous |

## Exemple cURL

```bash
# Connexion admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@esprittech.tn","password":"Admin@1234"}'

# Import du référentiel (remplacer <TOKEN>)
curl -X POST http://localhost:8080/api/admin/referentiel/import \
  -H "Authorization: Bearer <TOKEN>" \
  -F "file=@src/main/resources/sample-referentiel.csv"

# Inscription d'un utilisateur présent dans le référentiel
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nom":"Hadjbrahim","prenom":"Nouha","email":"nouha.hadjbrahim@esprit.tn","identifiant":"ETU-1001","password":"Passw0rd!"}'
```

## Sécurité — notes

- API **stateless** : pas de session, authentification par JWT à chaque requête.
- Le `JwtAuthenticationFilter` lit `Authorization: Bearer <token>`, valide la
  signature/expiration et rejette les refresh tokens pour l'accès aux ressources.
- Le refresh token est un JWT signé portant le claim `type=refresh` (stratégie
  stateless ; pour une révocation fine, basculer vers une table dédiée).
- `JWT_SECRET` doit faire **au moins 32 octets** (sinon l'application refuse de démarrer).
- En production : remplacer `ddl-auto=update` par une migration (Flyway/Liquibase),
  changer le mot de passe admin, restreindre les origines CORS.
