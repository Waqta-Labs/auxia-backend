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
Cross-cutting code (domain error categories, domain events, error responses,
OpenAPI, i18n, security, clock) lives in `shared`.

```text
src/main/java/com/waqtalabs/auxia/
├── AuxiaApplication.java
├── emergencymanagement/
│   ├── domain/
│   │   ├── model/aggregates/        # Emergency, Zone, FieldReport
│   │   ├── model/valueobjects/      # identifiers, PopulationImpact, UrgencyScore, enums…
│   │   ├── events/                  # ZoneRegistered, FieldReportRegistered…
│   │   ├── exceptions/              # EmergencyNotFound, EmergencyClosed…
│   │   ├── repositories/            # repository interfaces
│   │   └── services/                # ZoneIdentificationService, UrgencyScoringService
│   ├── application/                 # commands, queries, handlers, event handlers, ports
│   ├── interfaces/rest/             # controllers, resources and assemblers
│   └── infrastructure/
│       ├── persistence/jpa/
│       └── integration/aiservice/   # Anti-Corruption Layer to auxia-ai-service
├── resourcemanagement/
│   ├── domain/                      # Inventory and Brigade with their entities, exceptions
│   ├── application/                 # commands, handlers, InventoryQueryService, BrigadeQueryService
│   ├── interfaces/
│   │   ├── rest/                    # InventoryController, BrigadeController
│   │   └── acl/                     # ResourceManagementContextFacade for other contexts
│   └── infrastructure/persistence/jpa/
├── identityaccess/
│   ├── domain/                      # Organization and UserAccount, PasswordHashingService
│   ├── application/                 # sign-in, organizations, users, CheckAccessQueryHandler
│   ├── interfaces/
│   │   ├── rest/                    # AuthenticationController, OrganizationController, UserAccountController
│   │   └── acl/                     # IdentityAccessContextFacade for other contexts
│   └── infrastructure/
│       ├── persistence/jpa/
│       └── security/                # JWT issue and check, BCrypt, filter chain
└── shared/
    ├── domain/                      # error categories (404, 409, 422) and DomainEvent
    ├── application/                 # DomainEventPublisher, ExternalServiceUnavailable (503)
    ├── infrastructure/
    │   ├── events/                  # domain events through Spring, in the same process
    │   ├── documentation/openapi/   # OpenAPI metadata for Swagger UI
    │   ├── i18n/                    # Accept-Language resolution (en_US, es_419)
    │   ├── security/                # AuthenticatedUser, the principal of every request
    │   └── time/                    # clock used by the use cases
    └── interfaces/rest/             # global error handling (problem details)
```

## Configuration

The database, the AI Service and the security are configured with environment
variables:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL of the Neon database, without user and password, e.g. `jdbc:postgresql://<host>/neondb?sslmode=require` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `AI_SERVICE_URL` | URL of [auxia-ai-service](https://github.com/Waqta-Labs/auxia-ai-service). Default `http://localhost:8765` |
| `AI_SERVICE_API_KEY` | Shared secret sent in `X-API-Key`. It must be the same `AI_SERVICE_API_KEY` of the AI Service. Required |
| `AUTH_JWT_SECRET` | Base64 key that signs the access tokens, at least 32 bytes, e.g. `openssl rand -base64 32`. Required |
| `PLATFORM_ADMIN_KEY` | Secret of the platform operator, sent in `X-Platform-Key` to register and suspend organizations. Required |

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

Field reports are structured and urgency scores calculated by auxia-ai-service,
so start it too (see its README). Without it the API keeps working: reports are
registered without structured data and zones keep their previous score, and
both can be processed later (see [Urgency score](#urgency-score)).

## Testing

```bash
./mvnw verify
```

Docker must be running: the tests that start the application use a disposable
PostgreSQL 18 container (Testcontainers) and run every migration on it. Tests
never use Neon. Surefire points `DB_URL` to an unreachable address, so a test
that forgets the container fails instead of reaching Neon. It does the same
with `AI_SERVICE_URL`: tests never call the AI Service. The gateways are tested
against the responses of its contract with `MockRestServiceServer`, and the
tests that need structured reports or scores use `FakeAiService`, a
predictable double imported with `FakeAiServiceConfiguration`.

| Level | Where |
|---|---|
| Domain | `*Tests` next to each aggregate and value object, without Spring |
| Use cases | `*/application`, with in-memory repositories |
| Persistence and REST | `JpaRepositoriesTests`, `*ControllerTests` and the migration tests, on the container |
| Acceptance | `src/test/resources/features/*.feature` (Gherkin of chapter III), run by `AcceptanceTests` |

The endpoints need a signed-in user. Most REST tests and scenarios run as an
`AUTHORITY` (`DefaultAuthorityUserConfiguration`) or pick another user with
`TestUsers`. The Identity Access tests and the US-21 and US-22 scenarios sign in
through the API and send the real token instead.

## Endpoints

Every endpoint needs `Authorization: Bearer <token>`, except the sign-in, the
platform operator endpoints, Swagger UI and the health check. Reads are open to
any signed-in user; changes need a role (see [Roles](#roles)).

### Emergencies, zones and field reports (Emergency Management)

| Method and path | Description |
|---|---|
| `POST /api/v1/emergencies` | Register an emergency (`ACTIVE`). |
| `GET /api/v1/emergencies` | Active emergencies, from the most recent. |
| `GET /api/v1/emergencies/{id}` | One emergency. |
| `PATCH /api/v1/emergencies/{id}/close` | Close an emergency. It no longer accepts zones. |
| `POST /api/v1/emergencies/{emergencyId}/zones` | Register a zone of an active emergency. |
| `GET /api/v1/emergencies/{emergencyId}/zones` | Urgency ranking of the zones (US-06). Filters: `minScore`, `maxScore`, `need`. |
| `GET /api/v1/zones/{id}` | One zone, with its score and priority level. |
| `PATCH /api/v1/zones/{id}/population-impact` | Change some counts of the population impact; the others keep their value. Recalculates the score. |
| `GET /api/v1/zones/{id}/score` | Urgency score with its top features and model version, or why it is not available (TS-02, US-05). |
| `POST /api/v1/zones/{id}/score` | Recalculate the urgency score of a zone. |
| `GET /api/v1/zones/{id}/report` | PDF of the zone: impact, urgency score with the points of each variable, model version and field reports (US-05). |
| `POST /api/v1/zones/{zoneId}/field-reports` | Register a field report of a zone (TS-01). |
| `POST /api/v1/emergencies/{emergencyId}/field-reports` | Register a field report of an emergency, with or without `zoneId`. |
| `GET /api/v1/zones/{zoneId}/field-reports` | Reports of a zone, from the most recent. |
| `GET /api/v1/emergencies/{emergencyId}/field-reports` | Reports of an emergency; `?pendingZoneAssignment=true` lists only those without a zone. |
| `GET /api/v1/field-reports/{id}` | One report, with its structured data. |
| `POST /api/v1/field-reports/{id}/structure` | Structure a report that is not processed yet (US-02). |
| `PATCH /api/v1/field-reports/{id}/zone` | Assign the zone of a pending report. |

A field report always belongs to an emergency. Registered in an emergency
without `zoneId`, its zone is identified from the text: when exactly one zone
name of the emergency appears in it (ignoring case and accents), the report is
associated to that zone; otherwise it stays pending zone assignment until the
authority assigns it (US-01, scenario 2).

### Inventory (Resource Management)

| Method and path | Description |
|---|---|
| `POST /api/v1/inventories` | Create the inventory of the organization of the user (one per organization). |
| `GET /api/v1/inventories` | The inventory of the organization of the user. |
| `GET /api/v1/inventories/{id}` | Inventory with the on hand, reserved and available quantity of each resource (US-09, TS-03). Filter: `category`. |
| `POST /api/v1/inventories/{id}/resources` | Register a resource, without stock. |
| `POST /api/v1/inventories/{id}/resources/{resourceId}/restock` | Register the entry of a quantity received (US-08). |
| `PATCH /api/v1/inventories/{id}/resources/{resourceId}/adjustment` | Add or remove a quantity with a reason. |

- `available = onHand - reserved`: what is reserved for a distribution plan
  cannot be distributed again (US-09, scenario 2). `belowMinimum` tells when the
  available quantity is below the minimum stock of the resource.
- No operation takes more than the available quantity: an adjustment that
  removes too much answers 409 and changes nothing (TS-03). A restock or an
  adjustment of 0, or a negative restock, answers 400 (US-08, scenario 2).
- The reason of each adjustment is written to the log until the audit trail
  exists (QAD-03).
- The organization is the one of the signed-in user. An inventory of another
  organization answers 404.
- Other bounded contexts use `ResourceManagementContextFacade` in the same
  process: available stock of an organization, and reserving or releasing the
  resources of a distribution plan. A reservation holds every line or none.

### Brigades (Resource Management)

| Method and path | Description |
|---|---|
| `POST /api/v1/brigades` | Create a brigade of the organization of the user, with its base location. It starts `AVAILABLE`. |
| `GET /api/v1/brigades?status=` | Brigades of the organization of the user by name, optionally by status. |
| `GET /api/v1/brigades/{id}` | One brigade with its members and its assignment in progress. |
| `POST /api/v1/brigades/{id}/members` | Add a member (`LEADER`, `DRIVER`, `MEDIC` or `VOLUNTEER`). |
| `DELETE /api/v1/brigades/{id}/members/{memberId}` | Remove a member. |
| `PATCH /api/v1/brigades/{id}/availability` | Put the brigade `AVAILABLE` or `OFF_DUTY`. |

- Brigade names are unique per organization (409). A brigade of another
  organization answers 404.
- A brigade is assigned to a distribution plan only while it is available and
  has a `LEADER`, and has one assignment in progress at most. The domain
  already enforces these rules; the assignment itself arrives with the
  distribution plans and the brigade suggestion of the AI Service.
- While a brigade is assigned, its availability cannot change (409) and its last
  leader cannot be removed (422). `ASSIGNED` is never set by hand (422).

### Identity Access

| Method and path | Description |
|---|---|
| `POST /api/v1/auth/sign-in` | Sign in with email and password; returns the access token, its expiry, the role and the organization (US-21). |
| `POST /api/v1/organizations` | Register an organization with its first `ORGANIZATION_ADMIN`. Needs `X-Platform-Key`. |
| `PATCH /api/v1/organizations/{id}/status` | Suspend or reactivate an organization. Needs `X-Platform-Key`. |
| `GET /api/v1/organizations/{id}` | The organization of the user. |
| `POST /api/v1/organizations/{id}/users` | Register a user of the organization with a role (US-22). |
| `GET /api/v1/organizations/{id}/users` | Users of the organization, by email. |
| `GET /api/v1/users/me` | The account of the signed-in user. |
| `PATCH /api/v1/users/{id}/role` | Change the role of a user (US-22). |
| `PATCH /api/v1/users/{id}/status` | `ACTIVE` to enable or unlock an account, `DISABLED` to disable it. |

- Wrong email, wrong password, a locked account and a suspended organization
  get the same 401 "Invalid credentials.", so the answer does not reveal what
  was wrong (US-21). The fifth failed attempt in a row locks the account.
- Tokens are HS256 JWTs signed with `AUTH_JWT_SECRET` and valid for 8 hours.
  Each request reads the current account: a role change applies to the next
  request, and a disabled user or a suspended organization gets 401 at once,
  even with a token issued before (R-05).
- An administrator manages only the users of their organization (others
  answer 404) and never their own account (422). `LOCKED` is never set by hand.
- AuxIA has no platform administrator role: the platform operator registers
  organizations with `PLATFORM_ADMIN_KEY`.
- Other bounded contexts use `IdentityAccessContextFacade` to check the current
  role of a user before a critical command.

### Roles

| Role | Can change |
|---|---|
| `AUTHORITY` | Emergencies, zones, population impact, scores, field reports and their zone |
| `FIELD_BRIGADE` | Field reports (register and structure) |
| `ORGANIZATION_ADMIN` | Inventory, brigades and the users of the organization |
| `AUDITOR` | Nothing: reads only |

A role that cannot run an action gets 403.

## Urgency score

The AI Service structures the reports and calculates the scores; the backend
decides what to send and when (TS-C04).

- **Automatic.** Domain events keep everything up to date without the
  authority asking for it. A new report is structured with NLP, and the score
  of a zone is recalculated when the zone is registered, when one of its
  reports is structured and when a pending report is assigned to it. Each step
  runs after the commit, in its own transaction.
- **Most severe data.** The score receives, for each variable, the most severe
  value between the zone and its processed reports: the highest damage level,
  the largest counts and every need mentioned. The data registered for the zone
  does not change, and a new report never lowers its urgency.
- **Not available.** A zone without `population` has no score (US-04, scenario
  2): `GET /zones/{id}/score` answers `available: false` with
  `missingVariables: ["population"]`, and `POST` answers 422. Adding the
  population with `PATCH /zones/{id}/population-impact` calculates it.
- **AI Service down.** Registering never fails because of it: the report stays
  `processed: false` and the zone keeps its previous score.
  `POST /field-reports/{id}/structure` and `POST /zones/{id}/score` process
  them again, and answer 503 while the service is still down.
- **Explanation.** Every score keeps its `topFeatures`, the points each variable
  added, and the `modelVersion` that calculated it (US-05). The PDF of
  `GET /zones/{id}/report` includes them, in the language of `Accept-Language`
  (OpenPDF).

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
  more children than people affected), with a localized detail. When an
  external service such as the AI Service does not answer, the API answers 503.

- **Security.** A missing, invalid or expired token answers 401, and a role
  that cannot run the action answers 403, both as localized problem details.
  In Swagger UI, sign in with `POST /api/v1/auth/sign-in` and paste the token
  in **Authorize**.
