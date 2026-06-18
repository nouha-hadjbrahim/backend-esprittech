# Backend CI/CD Setup

This repository uses a GitLab CI/CD pipeline for:

1. Maven validation and tests
2. SonarQube analysis
3. Maven packaging
4. Docker image build and push to Docker Hub

## Pipeline Stages

### `validate`
- Runs `mvn validate`

### `test`
- Runs `mvn clean verify`
- Collects test reports and the `target/` directory as artifacts

### `sonar`
- Runs `mvn verify sonar:sonar -Dsonar.qualitygate.wait=true`
- Executes on `main` and merge request pipelines
- Uses the shared Maven and Sonar caches

### `package`
- Runs `mvn clean package -DskipTests`
- Publishes the generated Spring Boot JAR as an artifact

### `docker-build-push`
- Builds and pushes the Docker image to:
  `kmokaddem/catalogue_industrialisation_esprit-tech`
- Pushes two tags:
  - `latest`
  - `$CI_COMMIT_SHORT_SHA`
- Runs only on `main`

## Required GitLab CI/CD Variables

Add these variables in GitLab project settings under CI/CD > Variables:

- `SONAR_HOST_URL`
- `SONAR_TOKEN`
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

## How to Create a Docker Hub Access Token

1. Sign in to Docker Hub.
2. Open Account Settings.
3. Go to Security.
4. Create a new access token.
5. Store the token in GitLab as `DOCKERHUB_TOKEN`.

Use your Docker Hub username as `DOCKERHUB_USERNAME`.

## How to Create a SonarQube Token

1. Sign in to SonarQube.
2. Open your user profile.
3. Go to Security or Tokens.
4. Generate a new token.
5. Store it in GitLab as `SONAR_TOKEN`.

Set `SONAR_HOST_URL` to your SonarQube server URL, for example:

```text
https://sonarqube.example.com
```

The value must include `http://` or `https://`. `sonarqube.example.com:9000` by itself
will fail in the Sonar scanner.

## Run Maven Locally

```bash
mvn clean verify
```

## Run SonarQube Locally

```bash
mvn verify sonar:sonar -Dsonar.host.url="$SONAR_HOST_URL" -Dsonar.token="$SONAR_TOKEN" -Dsonar.qualitygate.wait=true
```

If you want to override the project key explicitly:

```bash
mvn verify sonar:sonar -Dsonar.projectKey=sonarQubeTest -Dsonar.host.url="$SONAR_HOST_URL" -Dsonar.token="$SONAR_TOKEN" -Dsonar.qualitygate.wait=true
```

## Build the Docker Image Locally

```bash
docker build -t kmokaddem/catalogue_industrialisation_esprit-tech:local .
```

## Push the Image Manually

```bash
docker login
docker tag kmokaddem/catalogue_industrialisation_esprit-tech:local kmokaddem/catalogue_industrialisation_esprit-tech:latest
docker tag kmokaddem/catalogue_industrialisation_esprit-tech:local kmokaddem/catalogue_industrialisation_esprit-tech:<commit-sha>
docker push kmokaddem/catalogue_industrialisation_esprit-tech:latest
docker push kmokaddem/catalogue_industrialisation_esprit-tech:<commit-sha>
```

## Notes

- The pipeline uses GitLab variables for secrets. Nothing sensitive is hardcoded.
- JaCoCo is already configured in `pom.xml`, and the XML report is written to:
  `target/site/jacoco/jacoco.xml`
