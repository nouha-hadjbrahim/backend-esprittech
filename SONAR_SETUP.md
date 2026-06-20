# SonarQube Setup

## Start SonarQube locally

```bash
docker compose -f docker-compose.sonar.yml up -d
```

## Open SonarQube

Open:

```text
http://localhost:9000
```

Default credentials:

```text
admin / admin
```

## Create a token

1. Sign in to SonarQube.
2. Open your profile or account menu.
3. Create a new token for this project.
4. Copy the token value immediately. It is shown once.

## Run local analysis

```bash
mvn clean verify sonar:sonar -Dsonar.token=YOUR_TOKEN
```

If you want to point to a custom SonarQube server:

```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN
```

## GitLab CI/CD variables

Create these protected or masked variables in GitLab:

- `SONAR_HOST_URL`
- `SONAR_TOKEN`

Recommended values:

- `SONAR_HOST_URL`: `http://your-sonarqube-host:9000`
- `SONAR_TOKEN`: the token generated in SonarQube

`SONAR_HOST_URL` must include the scheme. `localhost:9000` is not valid here; use
`http://localhost:9000` or `https://...`.

## Notes

- Do not commit tokens or passwords.
- Keep local overrides in ignored files such as `.env`.
- The project is configured to read JaCoCo XML from `target/site/jacoco/jacoco.xml`.
