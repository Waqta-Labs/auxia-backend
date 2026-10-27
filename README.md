# auxia-backend

AuxIA Backend API: Spring Boot (Java 21) modular monolith exposing RESTful APIs for emergency management, resource management, delivery traceability, identity &amp; access, and citizen transparency.

## Stack

| Item | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Build | Maven (wrapper included: `./mvnw`) |
| Database | PostgreSQL 18 on Neon, one schema per bounded context |
| Migrations | Flyway (`src/main/resources/db/migration`) |
| API docs | springdoc-openapi 3.2.0 (Swagger UI) |
| Tests | JUnit 6, Testcontainers (PostgreSQL 18), Cucumber 8 |

## Structure

One package per bounded context under `com.waqtalabs.auxia`, each with the
`domain`, `application`, `interfaces` and `infrastructure` layers. The domain
does not depend on Spring or JPA: JPA entities live in
`infrastructure/persistence/jpa` and a mapper turns them into aggregates.
Cross-cutting code (domain error categories, error responses, OpenAPI, i18n,
security, clock) lives in `shared`.

```text
src/main/java/com/waqtalabs/auxia/
├── AuxiaApplication.java
├── emergencymanagement/
│   ├── domain/
│   │   ├── model/aggregates/        # Emergency, Zone, FieldReport
│   │   ├── model/valueobjects/      # identifiers, PopulationImpact, Geolocation, enums
│   │   ├── exceptions/              # EmergencyNotFound, EmergencyClosed…
│   │   ├── repositories/            # repository interfaces
│   │   └── services/                # ZoneIdentificationService
│   ├── application/                 # commands, queries, command handlers, query services
│   ├── interfaces/rest/             # controllers, resources and assemblers
│   └── infrastructure/persistence/jpa/
└── shared/
    ├── domain/exceptions/           # NotFound (404), Conflict (409), RuleViolation (422)
    ├── infrastructure/
    │   ├── documentation/openapi/   # OpenAPI metadata for Swagger UI
    │   ├── i18n/                    # Accept-Language resolution (en_US, es_419)
    │   ├── security/                # security filter chain
    │   └── time/                    # clock used by the use cases
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

### Database migrations

Flyway owns the schema and Hibernate runs with `ddl-auto: validate`: it checks
the mappings and never creates or alters tables. Migrations run when the
application starts.

| Version | Change |
|---|---|
| V1 | Baseline: the five schemas of the project report (`assets/plant/SQL`). |
| V2 | `zones.population`: people living in the zone, optional. The urgency score needs it. |
| V3 | `field_reports.emergency_id`; `zone_id` becomes optional, so a report can be pending zone assignment (US-01). |

Neon already had the schema of V1 when Flyway was added, so on Neon V1 is
recorded as the baseline (`baseline-on-migrate`) and only V2 onwards run. An
empty database runs every migration from V1. The history table is
`public.flyway_schema_history`.

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

Docker must be running: the tests that start the application use a disposable
PostgreSQL 18 container (Testcontainers) and run every migration on it. Tests
never use Neon. Surefire points `DB_URL` to an unreachable address, so a test
that forgets the container fails instead of reaching Neon.

| Level | Where |
|---|---|
| Domain | `*Tests` next to each aggregate and value object, without Spring |
| Use cases | `emergencymanagement/application`, with in-memory repositories |
| Persistence and REST | `JpaRepositoriesTests`, `*ControllerTests` and the migration tests, on the container |
| Acceptance | `src/test/resources/features/*.feature` (Gherkin of chapter III), run by `AcceptanceTests` |

## Endpoints

| Method and path | Description |
|---|---|
| `POST /api/v1/emergencies` | Register an emergency (`ACTIVE`). |
| `GET /api/v1/emergencies` | Active emergencies, from the most recent. |
| `GET /api/v1/emergencies/{id}` | One emergency. |
| `PATCH /api/v1/emergencies/{id}/close` | Close an emergency. It no longer accepts zones. |
| `POST /api/v1/emergencies/{emergencyId}/zones` | Register a zone of an active emergency. |
| `GET /api/v1/emergencies/{emergencyId}/zones` | Zones of the emergency (urgency ranking from phase 2). |
| `GET /api/v1/zones/{id}` | One zone. |
| `POST /api/v1/zones/{zoneId}/field-reports` | Register a field report of a zone (TS-01). |
| `POST /api/v1/emergencies/{emergencyId}/field-reports` | Register a field report of an emergency, with or without `zoneId`. |
| `GET /api/v1/zones/{zoneId}/field-reports` | Reports of a zone, from the most recent. |
| `GET /api/v1/emergencies/{emergencyId}/field-reports` | Reports of an emergency; `?pendingZoneAssignment=true` lists only those without a zone. |
| `PATCH /api/v1/field-reports/{id}/zone` | Assign the zone of a pending report. |

A field report always belongs to an emergency. Registered in an emergency
without `zoneId`, its zone is identified from the text: when exactly one zone
name of the emergency appears in it (ignoring case and accents), the report is
associated to that zone; otherwise it stays pending zone assignment until the
authority assigns it (US-01, scenario 2).

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

  Domain errors answer 404 (not found), 409 (not allowed in the current
  state, such as closing a closed emergency) or 422 (a business rule, such as
  more children than people affected), with a localized detail.

- **Security.** Until the Identity Access phase every endpoint is open. That
  phase adds JWT authentication and roles.
