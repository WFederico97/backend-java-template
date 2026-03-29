# backend-java-core-template

Spring Boot 4.0.3 REST microservice scaffold implementing a layered hexagonal architecture. Java 17. Maven build. Dockerized with PostgreSQL, Redis, Kafka, and Grafana LGTM (Loki, Grafana, Tempo, Mimir) for full observability.

---

## Table of Contents

- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Layer Specification](#layer-specification)
  - [domain](#domain)
  - [application](#application)
  - [core](#core)
  - [infra](#infra)
- [Implemented Components](#implemented-components)
- [Configuration](#configuration)
  - [application.yml](#applicationyml)
  - [Environment Variables](#environment-variables)
  - [Docker Compose Services](#docker-compose-services)
  - [Dockerfile](#dockerfile)
- [API Surface](#api-surface)
- [Security](#security)
- [Observability](#observability)
- [Known Issues](#known-issues)
- [TODO](#todo)
- [Running](#running)

---

## Architecture

Hexagonal (ports and adapters) variant with four top-level packages:

```
domain        Pure domain model and domain exceptions. No framework imports (except JPA annotations on entities).
application   Use-case orchestration. Ports (in/out interfaces) and service implementations.
core          Cross-cutting concerns: configuration beans, constants, base classes, exception handling, security, messaging, web response envelope.
infra         Framework-bound adapters: inbound (controllers, DTOs) and outbound (repositories).
```

Dependency rule: `infra -> application -> domain`. `core` is referenced by all layers for shared infrastructure.

---

## Project Structure

```
src/main/java/wfederico/backendjavacoretemplate/
|
+-- BackendJavaCoreTemplateApplication.java          Entry point. @EnableJpaAuditing.
|
+-- domain/
|   +-- exception/
|   |   +-- BusinessLayerException.java              RuntimeException carrying HttpStatus.
|   +-- model/
|       +-- player/
|       |   +-- PlayerEntity.java                    JPA @Entity "players". Extends EntityBase.
|       +-- team/
|           +-- package-info.java                    Placeholder.
|
+-- application/
|   +-- port/
|   |   +-- in/
|   |   |   +-- package-info.java                    Inbound port interfaces (use-case contracts). Empty.
|   |   +-- out/
|   |       +-- package-info.java                    Outbound port interfaces (persistence contracts). Empty.
|   +-- service/
|       +-- PlayerService.java                       CRUD orchestration: findAll, findById, create.
|
+-- core/
|   +-- config/
|   |   +-- ModelMapperConfig.java                   Singleton ModelMapper bean.
|   |   +-- OpenApiConfig.java                       springdoc OpenAPI metadata.
|   +-- constants/
|   |   +-- ExceptionMessageConstants.java           Error message literals.
|   |   +-- RequestMessageConstants.java             Success message literals.
|   |   +-- ValidationConstants.java                 Bean-validation message literals.
|   +-- data/
|   |   +-- EntityBase.java                          @MappedSuperclass: createdAt, updatedAt (JPA auditing).
|   +-- exception/
|   |   +-- GlobalExceptionHandler.java              @RestControllerAdvice: Exception, MethodArgumentNotValidException, BusinessLayerException.
|   +-- messaging/
|   |   +-- KafkaConfig.java                         ProducerFactory, ConsumerFactory, KafkaTemplate beans. String keys, JSON values.
|   +-- security/
|   |   +-- SecurityConfig.java                      SecurityFilterChain. CSRF disabled. Stateless sessions. Swagger/actuator whitelisted. Injects SecurityFilter.
|   |   +-- SecurityFilter.java                      OncePerRequestFilter: IP-based rate limiting via Redis (10 req/60s), IP blacklist check.
|   +-- web/
|       +-- ApiResponseBase.java                     Generic envelope: status, message, data<T>, traceId.
|
+-- infra/
    +-- adapter/
    |   +-- in/
    |   |   +-- controller/
    |   |   |   +-- PlayerController.java            @RestController /api/v1/players. GET /{id}, POST.
    |   |   +-- dto/
    |   |       +-- PlayerRequestDTO.java            Inbound payload. @NotNull + @Pattern validations.
    |   |       +-- PlayerResponseDTO.java           Outbound payload. @JsonProperty snake_case mapping.
    |   +-- out/
    |       +-- repository/
    |           +-- PlayerRepository.java            JpaRepository<PlayerEntity, Long>.
    +-- entity/
        +-- package-info.java                        Placeholder (unused; entities reside in domain.model).
```

---

## Layer Specification

### domain

| Component | Description |
|---|---|
| `PlayerEntity` | JPA entity mapped to `players` table. Fields: `id` (IDENTITY), `firstName`, `lastName`, `position`, `alterPosition`. Inherits `createdAt`/`updatedAt` from `EntityBase`. |
| `BusinessLayerException` | Unchecked exception wrapping a message and `HttpStatus`. Thrown from service layer, caught by `GlobalExceptionHandler`. |
| `team/` | Empty package. Reserved for future `TeamEntity` aggregate. |

### application

| Component | Description |
|---|---|
| `PlayerService` | `@Service`. Injected dependencies: `PlayerRepository`, `ModelMapper`. Methods: `getAllPlayers()` returns `List<PlayerResponseDTO>` via stream-map. `getPlayerById(Long)` throws `BusinessLayerException(NOT_FOUND)` on empty Optional. `createPlayer(PlayerRequestDTO)` maps DTO to entity, persists, maps back. All read methods `@Transactional(readOnly = true)`. |
| `port.in/` | Empty. Intended for use-case interfaces (e.g., `PlayerUseCase`). Not yet wired. |
| `port.out/` | Empty. Intended for repository port interfaces. `PlayerService` currently depends directly on `PlayerRepository` (infra leak). |

### core

| Component | Description |
|---|---|
| `ModelMapperConfig` | Exposes a single `ModelMapper` bean. Reused across all services for entity-DTO mapping. |
| `OpenApiConfig` | Configures `springdoc-openapi` metadata: title, version, contact, license. |
| `EntityBase` | `@MappedSuperclass` with `@CreatedDate` / `@LastModifiedDate`. Requires `@EnableJpaAuditing` on the application class. |
| `GlobalExceptionHandler` | Three handlers: generic `Exception` (500), `MethodArgumentNotValidException` (400, field-error map), `BusinessLayerException` (dynamic status). All responses wrapped in `ApiResponseBase` with `traceId` from MDC. |
| `KafkaConfig` | Manual producer/consumer factory configuration. Reads `spring.kafka.bootstrap-servers`. Uses `StringSerializer`/`JsonSerializer` for producer, `StringDeserializer`/`JsonDeserializer` for consumer. `@SuppressWarnings("removal")` present due to deprecated Spring Kafka serializer APIs. |
| `SecurityConfig` | Disables CSRF. Stateless session policy. Whitelists `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`. All other requests currently `permitAll()` (placeholder). Registers `SecurityFilter` before `UsernamePasswordAuthenticationFilter`. Depends on `ObjectMapper` from `tools.jackson.databind` (Jackson 3.x internal package). |
| `SecurityFilter` | `OncePerRequestFilter`. Skips actuator/swagger paths. Implements: (1) IP blacklist lookup via `blacklist:{ip}` Redis key. Returns 403 with empty body if blacklisted. (2) IP rate limiting via `rate_limit:{ip}` Redis key with 60s TTL. Returns 429 with `ApiResponseBase` JSON body if >10 requests. |
| `ApiResponseBase<T>` | Lombok `@Builder` generic envelope. Fields: `status` (Integer), `message` (String), `data` (T), `traceId` (String). |
| Constants classes | `ExceptionMessageConstants`, `RequestMessageConstants`, `ValidationConstants` -- string literal centralization. Messages currently in Spanish. |

### infra

| Component | Description |
|---|---|
| `PlayerController` | `@RestController` at `/api/v1/players`. `GET /{id}` returns single player. `POST ""` accepts `@Valid @RequestBody PlayerRequestDTO`. Both endpoints wrap response in `ApiResponseBase` with traceId from MDC. Tagged for Swagger grouping. |
| `PlayerRequestDTO` | `@NotNull` on all fields. `@Pattern` regex on `firstName`, `lastName` (unicode letters, numbers, common punctuation). `@JsonProperty` snake_case serialization. |
| `PlayerResponseDTO` | Read-only projection. Declared `final class`. `@JsonProperty` snake_case. |
| `PlayerRepository` | `JpaRepository<PlayerEntity, Long>`. No custom query methods. |
| `infra.entity/` | Unused. `package-info.java` only. Entities are under `domain.model`. |

---

## Configuration

### application.yml

| Section | Key Configuration |
|---|---|
| DataSource | PostgreSQL via env vars. `ddl-auto: update`. `show-sql: true`. |
| Redis | Host/port via env vars. Password commented out. |
| Kafka | `bootstrap-servers` via env var. Consumer `group-id: my-microservice-group`, `auto-offset-reset: earliest`. |
| Server | Port from `APP_PORT` (default 8080). |
| springdoc | API docs at `/v3/api-docs`. Swagger UI at `/swagger-ui.html`. Sorted by method and alpha. |
| Actuator | Exposed endpoints: `health`, `info`, `prometheus`, `metrics`. Health detail: `always`. Prometheus export enabled. |
| OTLP | Metrics to `{OTEL_ENDPOINT}/v1/metrics` (10s step). Traces to `{OTEL_ENDPOINT}/v1/traces`. |
| Tracing | Sampling probability: `1.0`. |
| Logging | Correlation pattern includes `traceId`, `spanId` via MDC. |

### Environment Variables

Defined in `.env` / `.env.example`:

| Variable | Default | Purpose |
|---|---|---|
| `APP_PORT` | `8080` | Application and Tomcat port |
| `DB_HOST` | `postgres` | PostgreSQL hostname (Docker service name) |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `mi_base_de_datos` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `admin` | Database password |
| `REDIS_HOST` | `redis` | Redis hostname (Docker service name) |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BROKER` | `kafka:9092` | Kafka bootstrap server |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://grafana-lgtm:4318` | OpenTelemetry collector (Grafana LGTM) |
| `OTEL_SERVICE_NAME` | `my-microservice` | Service identifier in traces/metrics |

### Docker Compose Services

| Service | Image | Purpose | Healthcheck |
|---|---|---|---|
| `app` | Built from `Dockerfile` | Spring Boot application | Depends on postgres (healthy), redis, kafka, grafana-lgtm |
| `postgres` | `postgres:latest` | Primary datastore | `pg_isready` |
| `redis` | `redis:latest` | Rate limiting / caching / blacklist | None |
| `kafka` | `apache/kafka:latest` | Event streaming (KRaft mode, single node) | None |
| `grafana-lgtm` | `grafana/otel-lgtm:latest` | Grafana + Loki + Tempo + Mimir. OTLP receiver on 4317 (gRPC) / 4318 (HTTP). UI on 3000. | None |

Volumes: `postgres_data` mounted at `/var/lib/postgresql`, `redis_data` at `/data`.

### Dockerfile

Multi-stage build:
1. **Build stage**: `maven:3.9-eclipse-temurin-17`. Dependency resolution offline, then `mvn clean package -DskipTests`.
2. **Runtime stage**: `eclipse-temurin:17-jre`. Copies fat JAR. Exposes 8080.

---

## API Surface

Base path: `/api/v1/players`

| Method | Path | Request Body | Response | Status |
|---|---|---|---|---|
| `GET` | `/api/v1/players/{id}` | -- | `ApiResponseBase<PlayerResponseDTO>` | 200 / 404 |
| `POST` | `/api/v1/players` | `PlayerRequestDTO` (JSON) | `ApiResponseBase<PlayerResponseDTO>` | 201 / 400 |

`GET /api/v1/players` (list all) is implemented in `PlayerService.getAllPlayers()` but **not exposed** via a controller endpoint.

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Security

- CSRF disabled (stateless API).
- Session policy: `STATELESS`.
- All endpoints currently `permitAll()` -- authorization rules are a TODO.
- `SecurityFilter` (pre-auth filter):
  - **IP blacklist**: checks `blacklist:{ip}` key in Redis. If present, returns HTTP 403 with empty body (no JSON envelope).
  - **Rate limiting**: increments `rate_limit:{ip}` key in Redis. TTL 60s set on first request. If count > 10, returns HTTP 429 with `ApiResponseBase` JSON body.
- `ObjectMapper` imported from `tools.jackson.databind` (Jackson 3.x internal API, shipped with Spring Boot 4.0.3). Spring's auto-configured `ObjectMapper` bean is of this type.

---

## Observability

| Signal | Exporter | Destination |
|---|---|---|
| Metrics | OTLP (micrometer-registry-otlp) + Prometheus (`/actuator/prometheus`) | Grafana Mimir via OTLP; Prometheus scrape endpoint available |
| Traces | OTLP (spring-boot-starter-opentelemetry) | Grafana Tempo via OTLP |
| Logs | Stdout with traceId/spanId correlation pattern | Grafana Loki (collected by LGTM container) |

Sampling probability is `1.0` (100%). `traceId` is injected into all `ApiResponseBase` responses via `MDC.get("traceId")`.

Actuator endpoints exposed: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`, `/actuator/metrics`.

---

## Known Issues

1. **`SecurityConfig` / `SecurityFilter`**: imports `tools.jackson.databind.ObjectMapper` (Jackson 3.x). Spring Boot 4.x auto-configures a bean of this type. If the import is changed to `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2.x), the bean will not be found at runtime.
2. **Datadog metrics dependency**: `micrometer-registry-datadog` is pulled transitively. If `management.datadog.metrics.export.api-key` is not set, the application fails to start with `apiKey was 'null' but it is required`. Must either provide the key or exclude the auto-configuration.
3. **Postgres volume**: `compose.yaml` mounts at `/var/lib/postgresql` (not `/var/lib/postgresql/data`). PostgreSQL 18+ images expect this mount point for `pg_ctlcluster` compatibility, but existing data at `/var/lib/postgresql/data` from older images causes a startup error. Delete the volume on image upgrade.
4. **`infra.entity` package**: contains only `package-info.java`. Entities are defined in `domain.model.*`. The package is vestigial.
5. **Hexagonal port interfaces not wired**: `application.port.in` and `application.port.out` are empty. `PlayerService` directly depends on `PlayerRepository` (infrastructure adapter), violating the ports-and-adapters dependency rule.
6. **`getAllPlayers()` not exposed**: `PlayerService.getAllPlayers()` exists but no `@GetMapping` in the controller invokes it.
7. **Blacklist 403 response**: returns empty body (no `ApiResponseBase` JSON), inconsistent with the 429 rate-limit response.
8. **GraphQL schemas empty**: `src/main/resources/graphql/` and `src/main/resources/graphql-client/` are empty. `spring-boot-starter-graphql` and DGS codegen plugin are configured but non-functional.

---

## TODO

### Architecture / Structure

- [ ] Define inbound port interfaces in `application.port.in` (e.g., `PlayerUseCase`) and have `PlayerService` implement them.
- [ ] Define outbound port interfaces in `application.port.out` (e.g., `PlayerPersistencePort`) and have `PlayerRepository` (or a repository adapter) implement them.
- [ ] Remove or repurpose `infra.entity` package.
- [ ] Decouple `PlayerService` from `PlayerRepository` by injecting the outbound port interface instead.

### API / Controller

- [ ] Expose `GET /api/v1/players` endpoint in `PlayerController` for `getAllPlayers()`.
- [ ] Implement `PUT /api/v1/players/{id}` (update).
- [ ] Implement `DELETE /api/v1/players/{id}` (delete).
- [ ] Implement `PATCH /api/v1/players/{id}` (partial update) if needed.
- [ ] Add pagination support (`Pageable`, `Page<PlayerResponseDTO>`) to list endpoint.

### Security

- [ ] Replace `anyRequest().permitAll()` with proper authorization rules (role-based, scope-based, or endpoint-specific).
- [ ] Implement OAuth2 resource server JWT validation (starters are already declared in `pom.xml`).
- [ ] Return `ApiResponseBase` JSON body on blacklist 403 response for consistency.
- [ ] Make rate-limit threshold and window configurable via properties (currently hardcoded: 10 requests / 60 seconds).

### Messaging / Kafka

- [ ] Implement Kafka producer service (publish domain events).
- [ ] Implement Kafka consumer listener (`@KafkaListener`).
- [ ] Replace deprecated `JsonSerializer` / `JsonDeserializer` when Spring Kafka provides stable alternatives.
- [ ] Add topic configuration beans.
- [ ] Add consumer `group-id` configuration to `KafkaConfig` consumer factory (currently only in `application.yml`).

### GraphQL

- [ ] Add GraphQL schema files to `src/main/resources/graphql/`.
- [ ] Add remote service schemas to `src/main/resources/graphql-client/` for DGS codegen.
- [ ] Implement GraphQL resolvers / data fetchers.

### Observability

- [ ] Resolve Datadog `apiKey` startup error: either exclude `DatadogMetricsExportAutoConfiguration` or provide the key.
- [ ] Verify OTLP traces appear in Grafana Tempo.
- [ ] Verify OTLP metrics appear in Grafana Mimir.
- [ ] Verify log correlation (traceId/spanId) in Grafana Loki.
- [ ] Configure sampling probability < 1.0 for production profiles.
- [ ] Add custom `@Observed` / `@Timed` annotations on service methods for granular metrics.

### Data / Persistence

- [ ] Implement `TeamEntity` in `domain.model.team`.
- [ ] Define entity relationships (`@ManyToOne`, `@OneToMany`) between `PlayerEntity` and `TeamEntity`.
- [ ] Replace `ddl-auto: update` with Flyway or Liquibase for production migrations.
- [ ] Add database indexes on frequently queried columns.
- [ ] Implement Redis caching (`@Cacheable`) on read operations.

### Testing

- [ ] Add unit tests for `PlayerService`.
- [ ] Add integration tests for `PlayerController` (`@WebMvcTest` or `MockMvc`).
- [ ] Add repository tests (`@DataJpaTest`).
- [ ] Configure Testcontainers for PostgreSQL, Redis, Kafka integration tests.
- [ ] Implement Spring REST Docs snippets (Asciidoctor plugin pending Spring Boot 4 compatibility).

### Build / Deployment

- [ ] Add `.env` to `.gitignore` (only `.env.example` should be committed).
- [ ] Add Spring Boot profiles (`dev`, `staging`, `prod`) with profile-specific `application-{profile}.yml`.
- [ ] Configure CI/CD pipeline.
- [ ] Add health check to `app` service in `compose.yaml`.
- [ ] Pin Docker image versions (replace `latest` tags).

---

## Running

### Prerequisites

- Docker and Docker Compose
- Java 17 (for local development without Docker)
- Maven 3.9+ (or use included `mvnw`)

### Docker Compose

```bash
cp .env.example .env    # adjust values as needed
docker compose up --build
```

Services:
- Application: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Grafana: `http://localhost:3000`
- Prometheus metrics: `http://localhost:8080/actuator/prometheus`

### Local (without Docker)

Requires PostgreSQL, Redis, and Kafka running locally. Set environment variables or rely on `application.yml` defaults.

```bash
./mvnw spring-boot:run
```

