# auxia-backend

AuxIA Backend API: Spring Boot (Java 21) modular monolith exposing RESTful APIs for emergency management, resource management, delivery traceability, identity &amp; access, and citizen transparency.

## Stack

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Build | Maven (wrapper included: `./mvnw`) |
| Database | PostgreSQL on Neon, one schema per bounded context |
| API docs | springdoc-openapi 3.2.0 (Swagger UI) |

## Structure

One package per bounded context under `com.waqtalabs.auxia`, each with the
`domain`, `application`, `interfaces` and `infrastructure` layers. Cross-cutting
code (error responses, OpenAPI, i18n, security) lives in `shared`.

```text
src/main/java/com/waqtalabs/auxia/
├── AuxiaApplication.java
└── shared/
    ├── infrastructure/
    │   ├── documentation/openapi/   # OpenAPI metadata for Swagger UI
    │   ├── i18n/                    # Accept-Language resolution (en_US, es_419)
    │   └── security/                # security filter chain
    └── interfaces/rest/             # global error handling (problem details)
```

## Configuration

The database connection is read from environment variables:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL of the Neon database, without user and password, e.g. `jdbc:postgresql://<host>/neondb?sslmode=require` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |

For local development, copy `.env.example` to `.env` and fill in the values.
The application imports `.env` from the working directory when it exists.
`.env` is ignored by git and must never be committed.

The tables already exist in Neon, so Hibernate runs with `ddl-auto: validate`:
it checks the mappings and never creates or alters tables.

### Profiles

| Profile | Use |
|---|---|
| `dev` (default) | Local development. `/actuator/health` shows the status of each component, including the database. |
| `prod` | Deployment. Health details are hidden and the port is read from `PORT` (default `8080`). |

Activate a profile with `SPRING_PROFILES_ACTIVE=prod`.

## Running

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`.

| URL | Description |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI document |
| http://localhost:8080/actuator/health | Health check (database connection included) |
| http://localhost:8080/actuator/info | Application name and version |

## Testing

```bash
./mvnw verify
```

`AuxiaApplicationTests` starts the full application, so it needs the database
variables (or the `.env` file). It only validates the schema and never writes
to the database.

## API conventions

- **Language.** The API answers in English (`en_US`) by default and in Latin
  American Spanish (`es_419`) when the `Accept-Language` header asks for any
  Spanish variant (`es`, `es-PE`, `es-419`…). Texts live in
  `src/main/resources/messages*.properties`.
- **Errors.** Every error is an RFC 9457 problem detail
  (`application/problem+json`). Validation errors add the list of invalid
  fields:

  ```json
  {
    "title": "Invalid request",
    "status": 400,
    "detail": "One or more fields are invalid.",
    "instance": "/api/v1/emergencies",
    "errors": [
      { "field": "name", "message": "must not be blank" }
    ]
  }
  ```

- **Security.** Until the Identity Access phase every endpoint is open. That
  phase adds JWT authentication and roles.
