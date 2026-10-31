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
| Evidence storage | Azure Blob Storage (`azure-storage-blob` 12.36.0, over the JDK HTTP client) |
| Blockchain | Web3j 6.0.0, `DeliveryRegistry` on Sepolia ([auxia-smart-contracts](https://github.com/Waqta-Labs/auxia-smart-contracts)) |
| Tests | JUnit 6, Testcontainers (PostgreSQL 18, Azurite, Anvil), Cucumber 8 |

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
│   │   ├── model/aggregates/        # Emergency, Zone, FieldReport, DistributionPlan
│   │   ├── model/valueobjects/      # identifiers, PopulationImpact, UrgencyScore, enums…
│   │   ├── events/                  # ZoneRegistered, FieldReportRegistered…
│   │   ├── exceptions/              # EmergencyNotFound, EmergencyClosed…
│   │   ├── repositories/            # repository interfaces
│   │   └── services/                # ZoneIdentificationService, UrgencyScoringService,
│   │                                # DistributionOptimizationService
│   ├── application/                 # commands, queries, handlers, event handlers, ports
│   ├── interfaces/
│   │   ├── rest/                    # controllers, resources and assemblers
│   │   ├── acl/                     # EmergencyManagementContextFacade for other contexts
│   │   └── events/                  # consumer of DeliveryConfirmed (plan and zone)
│   └── infrastructure/
│       ├── persistence/jpa/
│       └── integration/
│           ├── aiservice/           # Anti-Corruption Layer to auxia-ai-service
│           ├── resourcemanagement/  # inventory, reservations and brigades, through its facade
│           └── identityaccess/      # current role of a user, through its facade
├── resourcemanagement/
│   ├── domain/                      # Inventory and Brigade with their entities, exceptions,
│   │                                # StaffAssignmentService
│   ├── application/                 # commands, handlers, InventoryQueryService, BrigadeQueryService
│   ├── interfaces/
│   │   ├── rest/                    # InventoryController, BrigadeController
│   │   ├── acl/                     # ResourceManagementContextFacade and public events
│   │   └── events/                  # consumer of DeliveryConfirmed (stock and brigade)
│   └── infrastructure/
│       ├── persistence/jpa/
│       └── integration/aiservice/   # brigade suggestion of auxia-ai-service
├── traceability/
│   ├── domain/                      # Delivery with DeliveryItem and Evidence, IntegrityRecord,
│   │                                # CanonicalDeliveryRecord, HashingService
│   ├── application/                 # register, anchor, verify; anchoring retry scheduler; ports
│   ├── interfaces/
│   │   ├── rest/                    # DeliveryController, DeliveryVerificationController
│   │   ├── acl/                     # public DeliveryConfirmed event
│   │   └── events/                  # consumer of BrigadeAssigned
│   └── infrastructure/
│       ├── persistence/jpa/
│       ├── hashing/                 # SHA-256
│       ├── storage/                 # Azure Blob Storage
│       ├── blockchain/              # DeliveryRegistry through Web3j
│       └── integration/             # Identity Access and Emergency Management facades
├── identityaccess/
│   ├── domain/                      # Organization and UserAccount, PasswordHashingService
│   ├── application/                 # sign-in, organizations, users, CheckAccessQueryHandler
│   ├── interfaces/
│   │   ├── rest/                    # AuthenticationController, OrganizationController, UserAccountController
│   │   └── acl/                     # IdentityAccessContextFacade for other contexts
│   └── infrastructure/
│       ├── persistence/jpa/
│       └── security/                # JWT issue and check, BCrypt, filter chain
├── citizentransparency/
│   ├── domain/                      # read models ZonePublicStatus and EmergencyPublicView
│   ├── application/                 # projection handlers, PublicStatusQueryService
│   ├── interfaces/
│   │   ├── rest/                    # PublicStatusController, no sign-in
│   │   └── events/                  # consumers of Emergency Management and Traceability
│   └── infrastructure/
│       ├── persistence/jpa/
│       └── web/                     # Cache-Control of the public answers
└── shared/
    ├── domain/                      # error categories (404, 409, 422) and DomainEvent
    ├── application/                 # DomainEventPublisher, ExternalServiceUnavailable (503)
    ├── infrastructure/
    │   ├── aiservice/               # HTTP client of auxia-ai-service, shared by the gateways
    │   ├── events/                  # domain events through Spring, in the same process
    │   ├── documentation/openapi/   # OpenAPI metadata for Swagger UI
    │   ├── i18n/                    # Accept-Language resolution (en_US, es_419)
    │   ├── security/                # AuthenticatedUser, the principal of every request
    │   └── time/                    # clock used by the use cases
    └── interfaces/rest/             # global error handling (problem details)
```

## Configuration

The database, the AI Service, the security, the evidence storage and the
blockchain are configured with environment variables:

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL of the Neon database, without user and password, e.g. `jdbc:postgresql://<host>/neondb?sslmode=require` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `AI_SERVICE_URL` | URL of [auxia-ai-service](https://github.com/Waqta-Labs/auxia-ai-service). Default `http://localhost:8765` |
| `AI_SERVICE_API_KEY` | Shared secret sent in `X-API-Key`. It must be the same `AI_SERVICE_API_KEY` of the AI Service. Required |
| `AUTH_JWT_SECRET` | Base64 key that signs the access tokens, at least 32 bytes, e.g. `openssl rand -base64 32`. Required |
| `PLATFORM_ADMIN_KEY` | Secret of the platform operator, sent in `X-Platform-Key` to register and suspend organizations. Required |
| `AZURE_STORAGE_CONNECTION_STRING` | Connection string of the Azure Storage account of the evidence files (portal: Storage account → Access keys). Locally, the one of Azurite (below). Required |
| `AZURE_STORAGE_CONTAINER` | Private container of the evidence files; created if it does not exist. Default `evidences` |
| `BLOCKCHAIN_RPC_URL` | JSON-RPC URL of the network, e.g. the Sepolia URL of Alchemy. Required |
| `BLOCKCHAIN_PRIVATE_KEY` | Private key of the wallet that owns `DeliveryRegistry`: only it can register deliveries. It pays the gas of each anchoring (SepoliaETH). Required |
| `BLOCKCHAIN_CONTRACT_ADDRESS` | Address of `DeliveryRegistry`. Default the Sepolia deployment, `0xe678347E182cbf00031401C97284AaB1C6b8148c` |
| `BLOCKCHAIN_CHAIN_ID` | Network of the transactions. Default `11155111` (Sepolia) |

For local development, copy `.env.example` to `.env` and fill in the values.
The application imports `.env` from the working directory when it exists.
`.env` is ignored by git and must never be committed.

Without an Azure account, run Azurite, the storage emulator of Microsoft. It
skips the API version check because the SDK can be newer than the emulator:

```bash
docker run -d --name azurite -p 10000:10000 mcr.microsoft.com/azure-storage/azurite \
  azurite-blob --blobHost 0.0.0.0 --skipApiVersionCheck --loose
```

and use its well-known development account:

```text
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;
```

Switching to Azure Blob Storage only changes that variable.

### Database migrations

Flyway owns the schema and Hibernate runs with `ddl-auto: validate`: it checks
the mappings and never creates or alters tables. Migrations run when the
application starts.

| Version | Change |
|---|---|
| V1 | Baseline: the five schemas of the project report (`assets/plant/SQL`). |
| V2 | `zones.population`: people living in the zone, optional. The urgency score needs it. |
| V3 | `field_reports.emergency_id`; `zone_id` becomes optional, so a report can be pending zone assignment (US-01). |
| V4 | `distribution_plans`: organization, coverage and the last modification (who, when, why); `distribution_items.recommended_quantity`, so the original recommendation survives a modification (US-13, US-14). |
| V5 | `deliveries.organization_id`; a `REGISTERED` delivery keeps its `transaction_hash` from the moment it is sent, while its anchoring is `PENDING`. |
| V6 | Projects once, into `citizen_transparency`, the emergencies and zones that existed before the public view, with the stage of their latest plan and delivery. |
| V7 | `field_reports`: the indicators corrected by an authority, next to those of the NLP, with who, when and why (US-03). `zones`: the manual priority with who, when and why (US-07). |
| V8 | `stock_alerts`: low stock alerts, one open at most per resource (US-10). |

Neon already had the schema of V1 when Flyway was added, so on Neon V1 is
recorded as the baseline (`baseline-on-migrate`) and only V2 onwards run. An
empty database runs every migration from V1. The history table is
`public.flyway_schema_history`.

### Profiles

| Profile | Use |
|---|---|
| `dev` (default) | Local development. `/actuator/health` answers `UP` or `DOWN` to anyone, and the status of each component, including the database, only to a signed-in user (`Authorization: Bearer <token>`). |
| `prod` | Deployment. Health details are hidden and the port is read from `PORT` (default `8080`). Swagger UI stays on, so the deployed API can be tried. |

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

Docker must be running: the tests that start the application use disposable
PostgreSQL 18 and Azurite containers (Testcontainers) and run every migration
on them. Tests never use Neon nor a real storage account. Surefire points `DB_URL` to an unreachable address, so a test
that forgets the container fails instead of reaching Neon. It does the same
with `AI_SERVICE_URL`: tests never call the AI Service. The gateways are tested
against the responses of its contract with `MockRestServiceServer`, and the
tests that need structured reports or scores use `FakeAiService`, a
predictable double imported with `FakeAiServiceConfiguration`. Nor a
blockchain: `BLOCKCHAIN_RPC_URL` is unreachable too, the delivery tests use
`FakeBlockchainLedger` (`FakeBlockchainConfiguration`), and the anchoring runs in
the request thread. The Web3j adapter is tested against the real
`DeliveryRegistry` bytecode, deployed on a local Anvil chain in a container.

| Level | Where |
|---|---|
| Domain | `*Tests` next to each aggregate and value object, without Spring |
| Use cases | `*/application`, with in-memory repositories |
| Persistence and REST | `JpaRepositoriesTests`, `*ControllerTests` and the migration tests, on the container |
| Acceptance | `src/test/resources/features/*.feature` (Gherkin of chapter III), run by `AcceptanceTests` |

The endpoints need a signed-in user. Most REST tests and scenarios run as an
`AUTHORITY` (`DefaultAuthorityUserConfiguration`) or pick another user with
`TestUsers`. The Identity Access tests and the US-21 and US-22 scenarios sign in
through the API and send the real token instead. So do the distribution plan
and delivery tests and the US-11 to US-18 scenarios, with `SignedInOrganization`,
because each decision asks Identity Access for the current role of a user who
must exist. `ApprovedPlan` prepares, through the API, an approved plan with its
brigade and its delivery open.

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
| `PATCH /api/v1/field-reports/{id}/structured-data` | Correct or complete the indicators of a structured report, with a reason (US-03). |
| `PUT /api/v1/zones/{id}/manual-priority` | Mark a zone as a priority by hand, with a justification (US-07). |
| `DELETE /api/v1/zones/{id}/manual-priority` | Take the manual priority off a zone. |

A field report always belongs to an emergency. Registered in an emergency
without `zoneId`, its zone is identified from the text: when exactly one zone
name of the emergency appears in it (ignoring case and accents), the report is
associated to that zone; otherwise it stays pending zone assignment until the
authority assigns it (US-01, scenario 2).

### Distribution plans (Emergency Management)

| Method and path | Description |
|---|---|
| `POST /api/v1/emergencies/{emergencyId}/distribution-plans` | Recommend a distribution of the inventory of the organization among the prioritized zones (US-11). |
| `GET /api/v1/emergencies/{emergencyId}/distribution-plans` | Plans of the emergency, from the most recent. Filter: `status`. |
| `GET /api/v1/zones/{zoneId}/distribution-plans` | History of the zone: every plan with its decision, modification and justifications (US-14). |
| `GET /api/v1/distribution-plans/{id}` | One plan, with the recommended and the current quantity of each resource. |
| `PATCH /api/v1/distribution-plans/{id}/items` | Change quantities of a proposed plan, with a reason (US-13, US-14). |
| `POST /api/v1/distribution-plans/{id}/approve` | Approve a proposed plan; no body, the authority comes from the token (US-12, TS-C01). |
| `POST /api/v1/distribution-plans/{id}/reject` | Reject a proposed plan with a reason (US-14). |
| `POST /api/v1/distribution-plans/{id}/brigade-assignment` | Assign a brigade to an approved plan that has none, because every brigade was busy at approval time. |

- A recommendation covers the whole emergency: the AI Service splits the
  available stock of the organization of the authority among every zone with an
  urgency score and identified needs, so zones compete for the same stock. Each
  zone that receives something gets one `PROPOSED` plan, and its stock is
  reserved at once, so two plans never count on the same units.
- The answer lists the zones left partially (`PARTIAL`) or totally (`NONE`)
  unattended for lack of inventory (US-11, scenario 2), and the zones left out:
  `NO_SCORE` (no urgency score yet) or `NO_NEEDS` (no processed field report
  mentions a need).
- While the organization has `PROPOSED` plans of an emergency, a new
  recommendation answers 409: approve or reject them first, so reserved stock
  always belongs to a visible plan.
- A zone with approved or executed plans can get another plan while it still
  has needs. The recommendation sends the AI Service what those plans already
  give the zone of each category (`alreadyAllocated`), which is taken from its
  demand: the zone only gets what it still lacks, and a zone already covered
  gets nothing. Rejected plans count for nothing.
- A modification lists the resources to change; the others keep their
  quantity, `0` takes a resource out and a new resource is added. Each line
  keeps its recommended quantity, so the original recommendation and the
  approved version are both kept (US-13, scenario 2). The reservation follows
  the new quantities, which cannot exceed the available stock (409).
- Rejecting releases the stock; an approved plan keeps it for the delivery. A
  plan is approved once (409 the second time). Modifying and rejecting need a
  reason (400), which stays in the history of the zone (US-14).
- Before each decision the use case asks Identity Access whether the user is
  still an active authority (QAD-02); if not, 403. Plans belong to the
  organization of the user; a plan of another organization answers 404.
- Approving a plan assigns it a brigade right after the commit: the AI Service
  suggests the available brigade with a leader closest to the zone; a zone
  without coordinates gets the first one by name. If none is free, or the AI
  Service does not answer, the plan stays approved without a brigade and
  `POST .../brigade-assignment` asks again (422 while none is free, 409 if the
  plan already has one). The assignment opens the delivery of the plan.
- When its delivery is registered, the plan becomes `EXECUTED` and its zone
  `SERVED`, with `lastAidReceivedAt`. An executed plan keeps counting in
  `alreadyAllocated`.

### Inventory (Resource Management)

| Method and path | Description |
|---|---|
| `POST /api/v1/inventories` | Create the inventory of the organization of the user (one per organization). |
| `GET /api/v1/inventories` | The inventory of the organization of the user. |
| `GET /api/v1/inventories/{id}` | Inventory with the on hand, reserved and available quantity of each resource (US-09, TS-03). Filter: `category`. |
| `POST /api/v1/inventories/{id}/resources` | Register a resource, without stock. |
| `POST /api/v1/inventories/{id}/resources/{resourceId}/restock` | Register the entry of a quantity received (US-08). |
| `PATCH /api/v1/inventories/{id}/resources/{resourceId}/adjustment` | Add or remove a quantity with a reason. |
| `GET /api/v1/inventories/{id}/alerts?status=` | Low stock alerts, the most recent first; `OPEN` or `RESOLVED` (US-10). |

- `available = onHand - reserved`: what is reserved for a distribution plan
  cannot be distributed again (US-09, scenario 2). `belowMinimum` tells when the
  available quantity is below the minimum stock of the resource.
- No operation takes more than the available quantity: an adjustment that
  removes too much answers 409 and changes nothing (TS-03). A restock or an
  adjustment of 0, or a negative restock, answers 400 (US-08, scenario 2).
- The reason of each adjustment is written to the log until the audit trail
  exists (QAD-03).
- Every change of stock (a new resource, a restock, an adjustment, a
  reservation and its change, release or consumption) evaluates the alerts
  after its commit: a resource whose available quantity falls below its minimum
  stock gets an `OPEN` alert, written to the log, and the alert is `RESOLVED`
  when the resource is restocked above the minimum. A new fall raises a new
  alert.
- The organization is the one of the signed-in user. An inventory of another
  organization answers 404.
- Other bounded contexts use `ResourceManagementContextFacade` in the same
  process: available stock of an organization, reserving, changing or releasing
  the resources of a distribution plan, and assigning it a brigade. A plan has
  one reservation; each operation applies every line or none.
- A registered delivery consumes the reservation of its plan: what was
  delivered leaves the warehouse (`onHand`) and the rest becomes available
  again.

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
  has a `LEADER`, and has one assignment in progress at most. It becomes
  `ASSIGNED` when a plan is approved and `AVAILABLE` again when the delivery of
  the plan is registered.
- While a brigade is assigned, its availability cannot change (409) and its last
  leader cannot be removed (422). `ASSIGNED` is never set by hand (422).

### Deliveries (Traceability)

| Method and path | Description |
|---|---|
| `GET /api/v1/deliveries?zoneId=` | Deliveries of a zone, from the most recent. |
| `GET /api/v1/deliveries/{id}` | One delivery with its items, evidence (by hash) and anchoring. |
| `POST /api/v1/deliveries/{id}/registration` | Register the delivery with its evidence (US-15, TS-05). `multipart/form-data`: a `delivery` part in JSON and one `evidences` part per file. |
| `GET /api/v1/deliveries/{id}/verify` | Public: compare the hash of the delivery as it is now with the one in the blockchain (US-17). |

- A delivery opens (`INITIATED`) when a brigade is assigned to an approved
  plan; each plan has one. A `FIELD_BRIGADE` member or an `AUTHORITY` of the
  organization registers it once; Identity Access confirms the role when the
  command runs.
- The registration needs at least one evidence file: without the `evidences`
  part it answers 400 and computes no hash (TS-05); an empty file answers 422.
  A file is a JPEG, PNG, WebP or HEIC photo or a PDF (422 otherwise). Each file
  is stored in Azure Blob Storage named by its SHA-256, which the delivery
  keeps. The quantities can be less than planned, never more nor other
  resources (422).
- The registration computes the SHA-256 of a canonical record with a closed
  list of fields: delivery, plan and zone identifiers, delivered quantities,
  hashes of the files and the delivery date. Names, documents and any other
  personal data have no field in the request nor in the record, so they never
  reach the blockchain (TS-C02, US-18).
- The field app may send a registration twice after losing the signal
  (TS-C06): a delivery already registered is returned as it is, with 200.
- The hash is anchored with `registerDelivery(deliveryId, hash)` right after the
  response, in the background: the 201 carries the hash, and the transaction
  shows up in `GET /deliveries/{id}` once it is sent. The delivery becomes
  `CERTIFIED` when the transaction is confirmed (US-16). If the network fails
  or rejects it, the anchoring is `FAILED`, and it is retried every 5 minutes,
  up to 10 transactions. The transaction is kept from the moment it is sent, so
  a retry checks it instead of sending the hash again.
- The verification needs no sign-in: the identifier of the delivery is its
  public code. It computes the hash again from the quantities in the database
  and the files read again from the storage, and compares it with
  `getDeliveryHash`. A changed quantity or a replaced photo gives
  `verified: false`. A code of no registered delivery answers 404, and 503 if
  the blockchain does not answer.

### Public view (Citizen Transparency)

| Method and path | Description |
|---|---|
| `GET /api/v1/public/emergencies` | Every emergency, the most recent first. |
| `GET /api/v1/public/emergencies/{id}/zones` | Zones of an emergency with their stage, by name. |
| `GET /api/v1/public/zones/{id}` | Stage of a zone and its delivery history (US-24, US-25). |
| `GET /api/v1/public/zones?district=&name=` | Zones of a district, optionally by part of their name, ignoring case. |

- No sign-in. The answers carry `Cache-Control: public, max-age=60`, so a CDN or
  the browser answers repeated queries: a change of stage shows, at most, a
  minute later (R-04).
- Stages: `REGISTERED`, `PRIORITIZED` (it has a score), `AID_APPROVED`,
  `AID_IN_TRANSIT` (its brigade is on the way) and `AID_DELIVERED`. Within one
  plan a zone only moves forward, even if an event arrives late or twice; a new
  plan starts again from `AID_APPROVED`. A rejected plan changes nothing.
- The history shows how many deliveries the zone received, the date of the last
  one and whether it is `VERIFIED` or `PENDING_VERIFICATION` in the blockchain.
  A verified last delivery adds `verificationUrl`, the public verification of
  Traceability, whose code is the identifier of the delivery (US-26).
- Never shown: personal data, evidence, the urgency score, the ranking or the
  delivered resources; crossed between zones they would reveal the internal
  prioritization (TS-C02, R-04). A zone not registered yet answers 404.
- The view is a read model: Citizen Transparency projects the public events of
  Emergency Management (`EmergencyRegistered`, `ZoneRegistered`, the score,
  `DistributionPlanApproved`) and of Traceability (`DeliveryInitiated`,
  `DeliveryConfirmed`, `DeliveryCertified`) after their commit.

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
| `AUTHORITY` | Emergencies, zones, population impact, scores, field reports and their zone, distribution plans and their brigade, deliveries |
| `FIELD_BRIGADE` | Field reports (register and structure), deliveries |
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
- **Manual corrections.** An authority corrects or completes the indicators of a
  structured report with a reason (US-03). The corrected values are used from
  then on and the score of the zone is calculated again; the report keeps the
  values of the NLP under `correction.originalData`, with who corrected them,
  when and why. A zone marked as a priority by hand, with its justification,
  goes first in the ranking and shows `manualPriority` (US-07).
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
