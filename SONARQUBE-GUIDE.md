# Guide complet SonarQube — EspritTECH Backend

Guide pas-a-pas pour analyser le code du projet avec SonarQube.
Chaque commande est donnee en detail. Suivre les etapes dans l'ordre.

---

## Pre-requis a verifier avant de commencer

### 1. JDK 17 ou superieur

Ouvrir un terminal (cmd ou PowerShell) et taper :

```cmd
java -version
```

Resultat attendu (la version doit etre >= 17) :

```
openjdk version "17.0.x" ...
```

Si Java n'est pas installe :
- Telecharger depuis : https://adoptium.net/
- Installer, puis verifier que `JAVA_HOME` pointe vers le dossier JDK :

```cmd
echo %JAVA_HOME%
```

Doit afficher quelque chose comme `C:\Program Files\Eclipse Adoptium\jdk-17...`

### 2. Git

```cmd
git --version
```

### 3. MySQL (XAMPP)

- Lancer XAMPP Control Panel
- Demarrer **MySQL** (le bouton Start)
- Le port doit etre **3306**

---

## Etape 1 — Cloner le projet et configurer l'environnement

### 1.1 Cloner le depot

```cmd
git clone https://gitlab.com/mokaddemkarim89/industrialisationprojets.git
cd industrialisationprojets
git checkout project-structure
```

### 1.2 Creer le fichier .env

Le fichier `.env` contient tous les secrets. Il n'est PAS dans Git.
Copier le template :

```cmd
copy .env.example .env
```

### 1.3 Remplir le fichier .env

Ouvrir `.env` avec un editeur et remplir les valeurs vides :

```properties
# --- JWT ---
JWT_SECRET=une-cle-secrete-tres-longue-au-moins-32-caracteres-pour-hmac-sha256
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# --- Base de donnees ---
DB_URL=jdbc:mysql://localhost:3306/esprittech?createDatabaseIfNotExist=true&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=
 
# --- Serveur ---
SERVER_PORT=8080

# --- CORS ---
CORS_ALLOWED_ORIGINS=http://localhost:4200

# --- Admin par defaut ---
ADMIN_EMAIL=admin@esprittech.tn
ADMIN_PASSWORD=Admin@1234
```

> **Note :** Si votre MySQL a un mot de passe, remplir `DB_PASSWORD=votre_mdp`.
> Avec XAMPP par defaut, root n'a pas de mot de passe : laisser `DB_PASSWORD=`

### 1.4 Verifier que le projet compile

```cmd
mvnw.cmd clean compile
```

Resultat attendu a la fin :

```
BUILD SUCCESS
```

Si erreur `DB_URL` ou `JWT_SECRET` : verifier que `.env` est bien rempli.

### 1.5 Verifier que les tests passent

```cmd
mvnw.cmd clean test
```

Resultat attendu :

```
Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Etape 2 — Installer SonarQube

### 2.1 Telecharger

Aller sur : https://www.sonarsource.com/products/sonarqube/downloads/

- Choisir **Community Edition** (gratuit)
- Cliquer sur **Download**
- Un fichier ZIP est telecharge (ex: `sonarqube-26.4.0.xxxxx.zip`)

### 2.2 Extraire

Extraire le ZIP dans un dossier simple (pas d'espaces ni d'accents dans le chemin) :

```
C:\sonarqube\
```

Apres extraction, la structure doit etre :

```
C:\sonarqube\
  bin\
    windows-x86-64\
      StartSonar.bat      <-- c'est ce fichier qu'on va lancer
  conf\
    sonar.properties
  data\
  logs\
  extensions\
  lib\
  temp\
```

> **Important :** Ne PAS extraire dans `C:\Program Files\` (les espaces causent des problemes).

---

## Etape 3 — Demarrer SonarQube

### 3.1 Lancer le serveur

Ouvrir un **nouveau terminal** (cmd) et taper :

```cmd
C:\sonarqube\bin\windows-x86-64\StartSonar.bat
```

> **IMPORTANT : Laisser ce terminal ouvert !** SonarQube tourne dedans.
> Si vous fermez le terminal, SonarQube s'arrete.

### 3.2 Attendre le demarrage

Le demarrage prend 1-3 minutes. Attendre jusqu'a voir dans les logs :

```
INFO  app[][o.s.a.SchedulerImpl] SonarQube is operational
```

### 3.3 Verifier dans le navigateur

Ouvrir : http://localhost:9000

La page de login SonarQube doit s'afficher.

### 3.4 Premier login

- **Login :** `admin`
- **Password :** `admin`

SonarQube demande immediatement de changer le mot de passe.
Choisir un nouveau mot de passe et le retenir (ex: `Sonar@1234`).

---

## Etape 4 — Generer un token d'analyse

Le token permet au projet d'envoyer ses resultats a SonarQube sans mot de passe.

### 4.1 Acceder aux parametres du compte

1. Dans SonarQube (http://localhost:9000), cliquer sur la lettre de votre profil **en haut a droite**
2. Cliquer sur **My Account**

### 4.2 Aller dans l'onglet Security

1. Cliquer sur l'onglet **Security**
2. Dans la section **Generate Tokens** :

| Champ | Valeur |
|-------|--------|
| Name  | `esprittech-local` |
| Type  | **Global Analysis Token** |
| Expires in | **No expiration** (ou 30 days) |

3. Cliquer sur le bouton **Generate**

### 4.3 Copier le token

Un token s'affiche, sous cette forme :

```
sqa_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Copier ce token immediatement** — il ne sera plus affiche apres avoir quitte la page.

Coller le token dans un fichier texte temporaire pour l'utiliser a l'etape suivante.

---

## Etape 5 — Lancer l'analyse

### 5.1 Ouvrir un terminal dans le dossier du projet

```cmd
cd C:\chemin\vers\industrialisationprojets
```

### 5.2 Executer la commande d'analyse

Remplacer `VOTRE_TOKEN_ICI` par le token copie a l'etape 4 :

```cmd
mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=VOTRE_TOKEN_ICI
```

Exemple concret :

```cmd
mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=sqa_eae5f062f2eee8d173ab1d5c832e414710e2627c
```

### 5.3 Ce que fait cette commande

| Phase | Description |
|-------|-------------|
| `clean` | Supprime les anciens fichiers compiles |
| `verify` | Compile le code, lance les 78 tests, genere le rapport de couverture JaCoCo |
| `sonar:sonar` | Envoie le code source + le rapport de couverture a SonarQube pour analyse |

### 5.4 Resultat attendu

A la fin de l'execution (2-3 minutes) :

```
[INFO] ANALYSIS SUCCESSFUL, you can find the results at: http://localhost:9000/dashboard?id=tn.esprit%3Abackend_EspritTech
[INFO] BUILD SUCCESS
```

Si vous voyez `BUILD FAILURE`, voir la section Depannage en bas.

---

## Etape 6 — Lire les resultats sur le dashboard

### 6.1 Ouvrir le dashboard

Cliquer sur le lien affiche dans le terminal, ou ouvrir :

http://localhost:9000/dashboard?id=tn.esprit%3Abackend_EspritTech

### 6.2 Comprendre les metriques

| Metrique | Signification | Objectif |
|----------|--------------|----------|
| **Bugs** | Erreurs dans le code qui peuvent causer un dysfonctionnement | 0 |
| **Vulnerabilities** | Failles de securite detectees automatiquement | 0 |
| **Security Hotspots** | Points du code a revoir manuellement (pas forcement des bugs) | Reviewed |
| **Code Smells** | Problemes de qualite/maintenabilite (pas des bugs) | Le moins possible |
| **Coverage** | Pourcentage du code couvert par les tests unitaires | 100% |
| **Duplications** | Pourcentage de code copie-colle | < 3% |

### 6.3 Explorer le detail

- Cliquer sur un chiffre (ex: "3 Code Smells") pour voir la liste
- Cliquer sur un probleme pour voir la ligne exacte dans le code
- Chaque probleme a une severite (Blocker, Critical, Major, Minor, Info)
- Chaque probleme a une explication et souvent une suggestion de correction

### 6.4 Consulter la couverture par fichier

1. Aller dans l'onglet **Measures**
2. Cliquer sur **Coverage**
3. La liste de tous les fichiers s'affiche avec leur pourcentage de couverture
4. Cliquer sur un fichier pour voir ligne par ligne ce qui est couvert (vert) ou non (rouge)

---

## Resume rapide (apres installation)

A chaque fois que vous modifiez le code et voulez relancer l'analyse :

```cmd
:: 1. Demarrer SonarQube (si pas deja lance)
C:\sonarqube\bin\windows-x86-64\StartSonar.bat

:: 2. Attendre 1-2 min, puis dans un AUTRE terminal :
cd C:\chemin\vers\industrialisationprojets
mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=VOTRE_TOKEN

:: 3. Ouvrir le dashboard
start http://localhost:9000/dashboard?id=tn.esprit%%3Abackend_EspritTech
```

---

## Commandes utiles

| Action | Commande |
|--------|----------|
| Compiler uniquement | `mvnw.cmd clean compile` |
| Lancer les tests uniquement | `mvnw.cmd clean test` |
| Lancer tests + analyse Sonar | `mvnw.cmd clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=VOTRE_TOKEN` |
| Voir le rapport JaCoCo en local | Ouvrir `target/site/jacoco/index.html` dans un navigateur |
| Demarrer SonarQube | `C:\sonarqube\bin\windows-x86-64\StartSonar.bat` |
| Arreter SonarQube | Appuyer `Ctrl+C` dans le terminal SonarQube |
| Verifier si SonarQube tourne | Ouvrir http://localhost:9000 dans le navigateur |

---

## Depannage

### "SonarQube can not be reached" / "Connection refused"

**Cause :** SonarQube n'est pas demarre.

**Solution :**
```cmd
:: Lancer SonarQube
C:\sonarqube\bin\windows-x86-64\StartSonar.bat

:: Attendre 1-2 minutes
:: Verifier : ouvrir http://localhost:9000 dans le navigateur
:: Relancer l'analyse une fois la page accessible
```

### "Not authorized" / "401"

**Cause :** Le token est invalide, expire ou mal copie.

**Solution :**
1. Aller sur http://localhost:9000 > My Account > Security
2. Generer un nouveau token
3. Relancer la commande avec le nouveau token

### "JAVA_HOME not set" / "java not found"

**Cause :** Java n'est pas installe ou pas configure.

**Solution :**
```cmd
:: Verifier l'installation
java -version

:: Si ca ne marche pas, installer JDK 17+ depuis https://adoptium.net/
:: Puis configurer JAVA_HOME :
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.x
```

> Pour rendre permanent : Panneau de configuration > Systeme > Variables d'environnement

### "BUILD FAILURE" lors de clean verify

**Cause possible 1 :** Fichier `.env` absent ou incomplet.

```cmd
:: Verifier que .env existe
dir .env

:: S'il n'existe pas, le creer depuis le template
copy .env.example .env
:: Puis remplir les valeurs (voir etape 1.3)
```

**Cause possible 2 :** MySQL n'est pas demarre.

```
Ouvrir XAMPP > Demarrer MySQL
```

**Cause possible 3 :** Le port 3306 est occupe par un autre service.

```cmd
:: Verifier quel processus utilise le port 3306
netstat -ano | findstr :3306
```

### Port 9000 deja occupe

**Cause :** Un autre service utilise le port 9000.

**Solution :** Modifier le port de SonarQube :
1. Ouvrir `C:\sonarqube\conf\sonar.properties`
2. Ajouter ou decommenter la ligne :
   ```
   sonar.web.port=9001
   ```
3. Relancer SonarQube
4. Utiliser `http://localhost:9001` partout a la place de `9000`

### "Elasticsearch storage path ... is a network filesystem"

**Cause :** Le dossier SonarQube est sur un disque reseau ou un partage.

**Solution :** Deplacer SonarQube sur un disque local (C:\sonarqube\).

---

## Structure des tests du projet

```
src/test/java/tn/esprit/backend_esprittech/
  |
  |-- SecurityIntegrationTest.java          14 tests  (Spring Boot + H2 en memoire)
  |-- BackendEspritTechApplicationTests.java 1 test
  |
  |-- controller/
  |     AuthControllerTest.java              4 tests   (register, login, refresh, me)
  |
  |-- service/
  |     AuthServiceTest.java                10 tests   (register, login, refresh + erreurs)
  |     UserServiceTest.java                 9 tests   (findById, findByEmail, getCurrentUser)
  |
  |-- config/
  |     JwtTokenProviderTest.java            7 tests   (generation, validation, expiration)
  |     JwtAuthenticationFilterTest.java     7 tests   (Bearer, refresh, invalide, etc.)
  |     CustomUserDetailsServiceTest.java    2 tests   (trouve, pas trouve)
  |     DataInitializerTest.java             2 tests   (admin existe, admin n'existe pas)
  |     SecurityConfigTest.java              3 tests   (beans de securite)
  |     CorsConfigTest.java                  1 test    (configuration CORS)
  |     DotenvConfigTest.java                1 test    (proprietes JWT)
  |
  |-- entity/
  |     UserEntityTest.java                  8 tests   (UserDetails, builder, onCreate)
  |
  |-- mapper/
  |     UserMapperTest.java                  2 tests   (mapping + null)
  |
  |-- exception/
  |     GlobalExceptionHandlerTest.java      6 tests   (401, 403, 409, 400, 500)
  |
  |-- dto/response/
        MessageResponseTest.java             1 test

TOTAL : 78 tests — 100% couverture de code
```
