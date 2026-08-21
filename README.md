# EventBooking

Backend REST API for **EventBooking** — a platform where organizers publish events at venues, and attendees browse and reserve seats.

Built as the final assignment of the LHIND TIA Java Internship.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Features](#features)
3. [Domain Model](#domain-model)
4. [Project Structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Configuration & Environment Variables](#configuration--environment-variables)
7. [Running the Application](#running-the-application)
8. [API Documentation (Swagger)](#api-documentation-swagger)
9. [Authentication](#authentication)
10. [API Reference](#api-reference)
11. [Usage Walkthrough (Examples)](#usage-walkthrough-examples)
12. [Health & Metrics](#health--metrics)
13. [Running the Tests](#running-the-tests)
14. [Business Rules Reference](#business-rules-reference)
15. [Troubleshooting](#troubleshooting)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 25 |
| Framework | Spring Boot 4.1.0 (Spring Framework 7) |
| Data access | Spring Data JPA / Hibernate 7 |
| Database | MySQL 8 (`dev`, `prod`) · H2 in-memory (`test`) |
| Security | Spring Security 7 + JWT (jjwt 0.12.6) |
| API docs | springdoc-openapi 3.0.2 (Swagger UI) |
| Logging | Log4j2 |
| Monitoring | Spring Boot Actuator |
| Config | spring-dotenv (`.env` support) |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc |
| Build | Maven |

---

## Features

**Authentication & Authorization**
- Stateless JWT authentication (register / login)
- Three roles: `ADMIN`, `ORGANIZER`, `ATTENDEE`
- Role-based access enforced with `@PreAuthorize` on controllers plus a central `SecurityFilterChain`
- Passwords hashed with BCrypt
- Deactivated accounts are blocked from logging in

**Public (no token required)**
- Browse and search published events — filter by city, category, date range and price range, with pagination and sorting
- View a single event with its venue, organizer, categories and average rating
- List categories
- View reviews for an event

**ATTENDEE**
- Book one or more seats for a published event
- View own bookings, optionally filtered by status
- Cancel own bookings (subject to the cancellation policy)
- Join a waitlist for a fully booked event
- Leave a rating and review for an attended event

**ORGANIZER**
- Create, update, publish and cancel own events
- View bookings made against own events
- Cannot touch events belonging to another organizer

**ADMIN**
- Manage venues and categories (create / update / delete)
- Activate or deactivate user accounts
- View all events, all bookings, all users
- Cancel any booking, cancel any event

**Cross-cutting**
- Centralised error handling (`@RestControllerAdvice`) returning a consistent `ErrorDTO`
- Optimistic locking (`@Version`) on `Event` to prevent seat overbooking under concurrency
- Automatic waitlist promotion when a confirmed booking is cancelled
- Multi-profile configuration (`dev` / `test` / `prod`)
- Health checks and metrics via Actuator

---

## Domain Model

Seven entities:

| Entity | Purpose |
|---|---|
| `User` | Account with `username`, `email`, hashed `password`, `role`, `active` flag |
| `Venue` | Physical location (`name`, `address`, `city`, `capacity`) |
| `Category` | Event category (`name`, unique) |
| `Event` | Event with schedule, price, seat counts, `status`, and `@Version` for optimistic locking |
| `Booking` | Seats reserved by a user for an event (`seatsBooked`, `status`, `bookingDate`) |
| `Review` | Rating + comment, backed by a confirmed booking |
| `Waitlist` | FIFO queue entry for a fully booked event (`position`, `status`) |

**Relationships** (derived from the assignment's business rules):

| Rule | Mapping |
|---|---|
| A venue hosts many events; each event is at exactly one venue | `Event.venue` → `@ManyToOne` (owning) · `Venue.events` → `@OneToMany(mappedBy = "venue")` |
| A user organizes many events; each event has exactly one organizer | `Event.organizer` → `@ManyToOne` · `User.organizedEvents` → `@OneToMany(mappedBy = "organizer")` |
| An event has many categories; a category applies to many events | `Event.categories` → `@ManyToMany` via `event_category` (owning) · `Category.events` → `@ManyToMany(mappedBy = "categories")` |
| A user makes many bookings; each booking belongs to one user | `Booking.user` → `@ManyToOne` · `User.bookings` → `@OneToMany(mappedBy = "user")` |
| An event has many bookings; each booking refers to one event | `Booking.event` → `@ManyToOne` · `Event.bookings` → `@OneToMany(mappedBy = "event")` |

All inverse (`@OneToMany` / `mappedBy`) sides are `FetchType.LAZY`. Collections use `@Builder.Default` so they are never `null` when an entity is created through the Lombok builder.

**Enums**

- `Role` — `ADMIN`, `ORGANIZER`, `ATTENDEE`
- `EventStatus` — `DRAFT`, `PUBLISHED`, `CANCELLED`
- `BookingStatus` — `CONFIRMED`, `CANCELLED`
- `WaitlistStatus` — `WAITING`, `PROMOTED`, `CANCELLED`

---

## Project Structure

```
src/main/java/com/isli/
├── EventBookingApplication.java   Spring Boot entry point
├── config/                        OpenApiConfig, SecurityConfig
├── controller/                    REST endpoints
├── dto/                           Request/response contracts
│   └── auth/                      RegisterRequestDTO, LoginRequestDTO, AuthDTO
├── entity/                        JPA entities + enums
├── exception/                     Custom exceptions + GlobalExceptionHandler
├── mapper/                        EntityMapper (entity → DTO)
├── repository/                    Spring Data JPA repositories
├── security/                      JwtTokenService, JwtAuthFilter, CustomUserDetailsService
└── service/                       Business logic

src/main/resources/
├── application.properties         Shared config (profile selection, JWT, actuator)
├── application-dev.properties     Local MySQL
├── application-test.properties    In-memory H2
├── application-prod.properties    Environment-driven
└── log4j2.xml                     Logging configuration

src/test/java/com/isli/
├── unit/                          Mockito-based service tests
└── integration/                   @SpringBootTest tests against H2
```

**Data access** uses all three querying styles required by the assignment:

- **Derived query methods** — e.g. `UserRepository.findByUsername`, `VenueRepository.findByCity`, `EventRepository.findByStatus`
- **JPQL** — `EventRepository.search(...)` (multi-filter search with pagination), `BookingRepository.sumConfirmedSeats(...)`
- **Native SQL** — `EventRepository.findAverageRating(...)`

---

## Prerequisites

- **JDK 25** (or the JDK version configured in `pom.xml`)
- **Maven 3.9+**
- **MySQL 8** running locally on port `3306` (only needed for the `dev` profile; `test` uses in-memory H2)

Verify:

```bash
java -version
mvn -version
```

---

## Configuration & Environment Variables

The application selects its profile from `SPRING_PROFILES_ACTIVE` and falls back to `dev`:

```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```

Variables can be supplied either as real environment variables **or** through a `.env` file at the project root (loaded automatically by `spring-dotenv`).

### `.env` file (recommended for local development)

Create a file named `.env` in the project root, next to `pom.xml`:

```dotenv
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=your-long-random-production-secret-at-least-32-chars
```

> **`.env` is listed in `.gitignore` and must never be committed.**

> **`JWT_SECRET` must be at least 32 characters (256 bits).** Shorter values cause the application to fail at startup with `WeakKeyException`, because HMAC-SHA signing requires a key of at least the hash output size (RFC 7518 §3.2).
>
> Generate one on Windows PowerShell:
> ```powershell
> [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
> ```

### Variables by profile

**Shared (all profiles)**

| Variable | Required | Default | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Which profile to activate |
| `JWT_SECRET` | Yes in prod | insecure placeholder | JWT signing key, min. 32 characters |
| `JWT_EXPIRATION_MS` | No | `86400000` (24 h) | Token lifetime in milliseconds |

**`dev` — local development against MySQL**

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_USERNAME` | No | `root` | MySQL username |
| `DB_PASSWORD` | No | `password` | MySQL password |

Connection URL is fixed in `application-dev.properties`:

```
jdbc:mysql://localhost:3306/eventbooking_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
```

- `createDatabaseIfNotExist=true` — the schema `eventbooking_dev` is created automatically on first run
- `allowPublicKeyRetrieval=true` — required for MySQL 8's `caching_sha2_password` plugin over a non-SSL connection
- `ddl-auto=update` — Hibernate creates and updates tables directly from the entities; no migration tool is used
- SQL logging is enabled (`show-sql=true`, `format_sql=true`) and application logging is at `DEBUG`

**`test` — automated tests**

No configuration or environment variables needed. Runs against in-memory H2 (`ddl-auto=create-drop`), so the schema is created fresh for every test run and destroyed afterwards. Activated automatically by `@ActiveProfiles("test")` in the integration tests.

**`prod` — production**

Everything comes from the environment; there are no defaults, so a missing variable fails fast at startup rather than silently using an insecure fallback.

| Variable | Required | Description |
|---|---|---|
| `DB_URL` | Yes | Full JDBC URL, e.g. `jdbc:mysql://db-host:3306/eventbooking_prod?useSSL=true&serverTimezone=UTC` |
| `DB_USERNAME` | Yes | Database username |
| `DB_PASSWORD` | Yes | Database password |
| `JWT_SECRET` | Yes | Signing key — must be overridden, min. 32 characters |
| `JWT_EXPIRATION_MS` | No | Token lifetime, defaults to 24 h |

Differences from `dev`:

- Swagger UI is **disabled** (`springdoc.swagger-ui.enabled=false`)
- SQL logging is off, application logging is at `WARN`

---

## Running the Application

### 1. Build

```bash
mvn clean package
```

This compiles the project, runs the test suite, and produces `target/EventBooking-1.0-SNAPSHOT.jar`.

To build without running tests:

```bash
mvn clean package -DskipTests
```

### 2. Run — `dev` profile (default)

Make sure MySQL is running, then:

```bash
java -jar target/EventBooking-1.0-SNAPSHOT.jar
```

or via the Maven plugin:

```bash
mvn spring-boot:run
```

The application starts on **http://localhost:8080**. Look for `Tomcat started on port 8080` in the log.

### 3. Run — `prod` profile

**Windows PowerShell**

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DB_URL="jdbc:mysql://localhost:3306/eventbooking_prod?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your-long-random-production-secret-at-least-32-chars"

java -jar target/EventBooking-1.0-SNAPSHOT.jar
```

**macOS / Linux**

```bash
SPRING_PROFILES_ACTIVE=prod \
DB_URL="jdbc:mysql://localhost:3306/eventbooking_prod?useSSL=false&serverTimezone=UTC" \
DB_USERNAME=root \
DB_PASSWORD=your_password \
JWT_SECRET=your-long-random-production-secret-at-least-32-chars \
java -jar target/EventBooking-1.0-SNAPSHOT.jar
```

Confirm the log shows `The following 1 profile is active: "prod"`, and that `/swagger-ui.html` returns 404 (docs are disabled in production).

---

## API Documentation (Swagger)

Once running under `dev`:

```
http://localhost:8080/swagger-ui.html
```

Raw OpenAPI definition:

```
http://localhost:8080/v3/api-docs
```

**Authorising requests in Swagger UI**

1. Call `POST /api/auth/register` or `POST /api/auth/login` and copy the `token` from the response
2. Click the **Authorize** button (top right)
3. Paste the token **without** the `Bearer ` prefix — Swagger adds it automatically
4. Protected endpoints can now be executed directly from the UI

**Paginated endpoints in Swagger.** Endpoints that accept `Pageable` expose `page`, `size` and `sort` as separate query fields. Leave `sort` empty, or use a real field name such as `title,asc` or `startDateTime,desc`. Sorting is only supported on fields selected by the query itself — nested paths like `venue.city` are rejected by Spring Data.

---

## Authentication

All protected endpoints expect a JWT in the `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens are **stateless** — they are never stored in the database. They are issued at register/login and carry the username, role, user ID, issued-at and expiry claims. If a token is lost, simply log in again to obtain a new one.

**Creating an ADMIN account.** Self-registration is restricted to `ATTENDEE` and `ORGANIZER`; attempting to register as `ADMIN` returns `400 Bad Request`. To create an administrator:

1. Register the user normally (as `ATTENDEE`)
2. Promote the account directly in the database:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE username = 'admin1';
   ```
3. Log in again — the new token will carry `role: ADMIN`

---

## API Reference

Legend: 🌐 public · 👤 ATTENDEE · 🎤 ORGANIZER · 🛡️ ADMIN

### Auth

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | 🌐 | Create an account, returns a JWT |
| `POST` | `/api/auth/login` | 🌐 | Authenticate, returns a JWT |

### Events

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/events` | 🌐 | Search published events (filters + pagination) |
| `GET` | `/api/events/{id}` | 🌐 | Event details incl. venue, organizer, categories, average rating |
| `POST` | `/api/events` | 🎤 | Create an event (starts as `DRAFT`) |
| `PUT` | `/api/events/{id}` | 🎤 | Update an owned event |
| `PATCH` | `/api/events/{id}/publish` | 🎤 | `DRAFT` → `PUBLISHED` |
| `PATCH` | `/api/events/{id}/cancel` | 🎤 🛡️ | Cancel an event (organizer: own only; admin: any) |
| `GET` | `/api/events/organizer/me` | 🎤 | List own events |

**Search query parameters for `GET /api/events`** — all optional:

| Parameter | Type | Example |
|---|---|---|
| `city` | string | `Tirana` |
| `categoryId` | long | `1` |
| `from` | ISO date-time | `2026-01-01T00:00:00` |
| `to` | ISO date-time | `2026-12-31T23:59:59` |
| `minPrice` | decimal | `0` |
| `maxPrice` | decimal | `100` |
| `page` | int | `0` |
| `size` | int | `10` |
| `sort` | string | `startDateTime,asc` |

### Bookings

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/bookings` | 👤 | Book seats for a published event |
| `GET` | `/api/bookings/me` | 👤 | Own bookings, optional `?status=CONFIRMED` |
| `PATCH` | `/api/bookings/{id}/cancel` | 👤 🛡️ | Cancel a booking (admin bypasses the time policy) |
| `GET` | `/api/bookings/organizer/events/{eventId}` | 🎤 | Bookings against an owned event |

### Waitlist

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/events/{eventId}/waitlist` | 👤 | Join the waitlist for a fully booked event |

### Reviews

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/reviews` | 👤 | Leave a rating/review for an attended event |
| `GET` | `/api/events/{eventId}/reviews` | 🌐 | Reviews for an event (paginated) |

### Venues

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/venues` | 🛡️ | List all venues |
| `GET` | `/api/venues/{id}` | 🛡️ | Venue details |
| `POST` | `/api/venues` | 🛡️ | Create a venue |
| `PUT` | `/api/venues/{id}` | 🛡️ | Update a venue |
| `DELETE` | `/api/venues/{id}` | 🛡️ | Delete a venue |

### Categories

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/categories` | 🌐 | List categories (needed for public search filtering) |
| `POST` | `/api/categories` | 🛡️ | Create a category |
| `PUT` | `/api/categories/{id}` | 🛡️ | Rename a category |
| `DELETE` | `/api/categories/{id}` | 🛡️ | Delete a category |

### Admin

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/admin/users` | 🛡️ | List all users (paginated) |
| `PATCH` | `/api/admin/users/{id}/activate` | 🛡️ | Reactivate an account |
| `PATCH` | `/api/admin/users/{id}/deactivate` | 🛡️ | Deactivate an account (blocks login) |
| `GET` | `/api/admin/events` | 🛡️ | All events regardless of organizer |
| `GET` | `/api/admin/events/by-status?status=DRAFT` | 🛡️ | Filter all events by status |
| `GET` | `/api/admin/bookings` | 🛡️ | All bookings in the system |
| `PATCH` | `/api/admin/bookings/{id}/cancel` | 🛡️ | Cancel any booking |

### Error responses

All errors return the same shape:

```json
{
  "timestamp": "2026-08-20T18:16:52.123",
  "status": 409,
  "error": "Conflict",
  "message": "Only 2 seat(s) left - join the waitlist instead",
  "path": "/api/bookings",
  "validationErrors": null
}
```

Validation failures populate `validationErrors`:

```json
{
  "timestamp": "2026-08-20T18:16:52.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "validationErrors": {
    "password": "size must be between 8 and 100"
  }
}
```

| Status | Raised by |
|---|---|
| `400` | Bean Validation failures, illegal arguments (e.g. `endDateTime` before `startDateTime`, self-registering as ADMIN) |
| `401` | Bad credentials |
| `403` | Missing/invalid token, wrong role, or acting on a resource you don't own; deactivated account |
| `404` | Referenced entity does not exist |
| `409` | Business-rule conflicts: seats unavailable, duplicate resource, invalid state transition, cancellation window passed, review not allowed |
| `500` | Unexpected errors (details are logged server-side, never exposed to the client) |

---

## Usage Walkthrough (Examples)

A complete run-through, in dependency order. Replace `<...>` placeholders with real values as you go.

### Step 1 — Create accounts

```bash
# Organizer
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"organizer1","password":"pass12345","email":"org@test.com","role":"ORGANIZER"}'

# Attendee
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"attendee1","password":"pass12345","email":"att1@test.com","role":"ATTENDEE"}'

# Future admin (registered as ATTENDEE, then promoted in the database)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"pass12345","email":"admin@test.com","role":"ATTENDEE"}'
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "organizer1",
  "role": "ORGANIZER"
}
```

Promote the admin, then log in again to get an `ADMIN` token:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'admin1';
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"pass12345"}'
```

Keep all three tokens — each set of endpoints needs the matching role.

### Step 2 — Set up a venue and a category (ADMIN)

```bash
curl -X POST http://localhost:8080/api/venues \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"name":"Palace of Congresses","address":"Bulevardi Deshmoret e Kombit","city":"Tirana","capacity":100}'

curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{"name":"Music"}'
```

Note the returned `id` of each — they are needed to create an event.

### Step 3 — Create and publish an event (ORGANIZER)

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ORGANIZER_TOKEN>" \
  -d '{
    "title":"Summer Concert",
    "description":"Open-air concert",
    "startDateTime":"2026-12-01T20:00:00",
    "endDateTime":"2026-12-01T22:00:00",
    "price":15,
    "totalSeats":2,
    "venueId":1,
    "categoryId":[1]
  }'
```

> `categoryId` is a **collection**, so it must always be a JSON array — `[1]`, not `1`, even for a single category.

The event is created with `status: "DRAFT"` and is not publicly visible yet. Publish it:

```bash
curl -X PATCH http://localhost:8080/api/events/1/publish \
  -H "Authorization: Bearer <ORGANIZER_TOKEN>"
```

Verify it is now publicly visible (no token needed):

```bash
curl "http://localhost:8080/api/events?page=0&size=10"
curl "http://localhost:8080/api/events?city=Tirana&minPrice=0&maxPrice=50&sort=startDateTime,asc"
```

### Step 4 — Book seats (ATTENDEE)

```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ATTENDEE_TOKEN>" \
  -d '{"eventId":1,"seatsBooked":2}'
```

Response:

```json
{
  "id": 1,
  "seatsBooked": 2,
  "status": "CONFIRMED",
  "bookingDate": "2026-08-20T18:30:00",
  "eventId": 1,
  "eventTitle": "Summer Concert",
  "userId": 2,
  "username": "attendee1"
}
```

The event now has `availableSeats: 0`. View your bookings:

```bash
curl "http://localhost:8080/api/bookings/me?page=0&size=10" \
  -H "Authorization: Bearer <ATTENDEE_TOKEN>"

curl "http://localhost:8080/api/bookings/me?status=CONFIRMED&page=0&size=10" \
  -H "Authorization: Bearer <ATTENDEE_TOKEN>"
```

### Step 5 — Waitlist and automatic promotion

Register a second attendee, then have them join the waitlist — the event is full, so direct booking is rejected:

```bash
curl -X POST http://localhost:8080/api/events/1/waitlist \
  -H "Authorization: Bearer <ATTENDEE2_TOKEN>"
```

```json
{ "id": 1, "status": "WAITING", "position": 1, "eventId": 1, "userId": 3 }
```

Now the first attendee cancels:

```bash
curl -X PATCH http://localhost:8080/api/bookings/1/cancel \
  -H "Authorization: Bearer <ATTENDEE_TOKEN>"
```

Two seats are returned to the event, and the waitlisted user is **automatically promoted** to a confirmed 1-seat booking — the waitlist entry moves to `PROMOTED` and the event ends up with 1 available seat. Confirm with:

```bash
curl "http://localhost:8080/api/bookings/me?page=0&size=10" \
  -H "Authorization: Bearer <ATTENDEE2_TOKEN>"
```

### Step 6 — Leave a review (ATTENDEE)

A review requires a **confirmed booking** and an event that has **already ended**. For testing before the event date, move the end date into the past:

```sql
UPDATE events SET end_date_time = '2020-01-01 22:00:00' WHERE id = 1;
```

```bash
curl -X POST http://localhost:8080/api/reviews \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ATTENDEE_TOKEN>" \
  -d '{"eventId":1,"rating":5,"comment":"Excellent show"}'
```

Read reviews publicly:

```bash
curl "http://localhost:8080/api/events/1/reviews?page=0&size=10"
```

The event's `averageRating` (computed with a native SQL query) now appears in `GET /api/events/1`.

### Step 7 — Admin oversight

```bash
curl "http://localhost:8080/api/admin/users?page=0&size=10" -H "Authorization: Bearer <ADMIN_TOKEN>"
curl "http://localhost:8080/api/admin/events?page=0&size=10" -H "Authorization: Bearer <ADMIN_TOKEN>"
curl "http://localhost:8080/api/admin/events/by-status?status=PUBLISHED" -H "Authorization: Bearer <ADMIN_TOKEN>"
curl "http://localhost:8080/api/admin/bookings?page=0&size=10" -H "Authorization: Bearer <ADMIN_TOKEN>"

# Deactivate an account — that user can no longer log in
curl -X PATCH http://localhost:8080/api/admin/users/2/deactivate -H "Authorization: Bearer <ADMIN_TOKEN>"

# Reactivate
curl -X PATCH http://localhost:8080/api/admin/users/2/activate -H "Authorization: Bearer <ADMIN_TOKEN>"

# Cancel any booking, bypassing the 24-hour policy
curl -X PATCH http://localhost:8080/api/admin/bookings/2/cancel -H "Authorization: Bearer <ADMIN_TOKEN>"
```

### Step 8 — Verifying the security rules

These should all be rejected:

```bash
# ATTENDEE trying to create a venue → 403
curl -X POST http://localhost:8080/api/venues \
  -H "Content-Type: application/json" -H "Authorization: Bearer <ATTENDEE_TOKEN>" \
  -d '{"name":"X","address":"Y","city":"Z","capacity":10}'

# No token on a protected endpoint → 403
curl http://localhost:8080/api/bookings/me

# Self-registering as ADMIN → 400
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"hacker","password":"pass12345","email":"h@test.com","role":"ADMIN"}'

# Publishing another organizer's event → 403
curl -X PATCH http://localhost:8080/api/events/1/publish \
  -H "Authorization: Bearer <OTHER_ORGANIZER_TOKEN>"
```

---

## Health & Metrics

| Endpoint | Access | Description |
|---|---|---|
| `GET /actuator/health` | 🌐 | Basic `UP`/`DOWN` status — reachable without a token so uptime checks and load balancers can use it |
| `GET /actuator/metrics` | 🛡️ | JVM, HTTP and connection-pool metrics |
| `GET /actuator/info` | 🛡️ | Build/application info |

```bash
curl http://localhost:8080/actuator/health
```

```json
{ "groups": ["liveness", "readiness"], "status": "UP" }
```

Detailed health components (including datasource status) are shown only to authorised users — `management.endpoint.health.show-details=when-authorized`.

---

## Running the Tests

```bash
mvn test
```

The suite runs against the `test` profile (in-memory H2) — **no MySQL instance is required**.

**Unit tests** (`src/test/java/com/isli/unit/`) — Mockito, no Spring context, fast:

- `AuthService` — registration rules (ADMIN blocked, duplicate username/email), login (wrong password, deactivated account)
- `EventService` — date validation, ownership checks, state transitions (`DRAFT` → `PUBLISHED` → `CANCELLED`)
- `BookingService` — seat decrement, insufficient seats, cancellation cutoff, admin bypass, waitlist promotion arithmetic
- `ReviewService` — eligibility (confirmed booking, event finished, no duplicate)
- `WaitlistService` — rejection when seats remain, FIFO position assignment

**Integration tests** (`src/test/java/com/isli/integration/`) — `@SpringBootTest` with a real (H2) database:

- `BookingFlowIntegrationTest` — a booking request flowing through validation, persistence and seat-availability updates; overbooking rejection; and the full cancel → waitlist-promotion cycle verified against the database
- `AuthControllerIntegrationTest` — the complete HTTP → controller → service → repository → Spring Security chain: register, log in, then use the issued JWT on a protected endpoint

---

## Business Rules Reference

| Rule | Implementation |
|---|---|
| Available seats must never be exceeded | `BookingService.book` checks `availableSeats`, plus an independent `sumConfirmedSeats` cross-check; `Event` carries `@Version` for optimistic locking against concurrent bookings |
| Cancellation policy | Attendees cannot cancel within **24 hours** of the event start (`CANCELLATION_HOURS`); admins bypass this |
| Waitlist promotion | When a confirmed booking is cancelled and seats free up, the lowest-`position` `WAITING` entry is promoted to a confirmed 1-seat booking and marked `PROMOTED` |
| Waitlist eligibility | Joining is only allowed when the event is fully booked; duplicate entries are rejected |
| Review eligibility | Requires a `CONFIRMED` booking for that event, the event must have already ended, and only one review per user per event (enforced in the service and by a unique constraint) |
| Event ownership | Organizers can only modify/cancel their own events; violations raise `ForbiddenOperationException` → `403` |
| Event lifecycle | New events start as `DRAFT` and are invisible to public search; only `DRAFT` events can be published; cancelled events cannot be updated |
| Account deactivation | Deactivated users are rejected at login with `AccountDisabledException` → `403` |
| ADMIN provisioning | `ADMIN` cannot be chosen at registration; the role must be assigned directly in the database |

---

## Troubleshooting

**`WeakKeyException: The specified key byte array is N bits...`**
`JWT_SECRET` is shorter than 32 characters. Set a longer value in `.env` or as an environment variable, then rebuild.

**`Public Key Retrieval is not allowed`**
The MySQL JDBC URL is missing `allowPublicKeyRetrieval=true`. This is required by MySQL 8's default `caching_sha2_password` plugin over a non-SSL connection.

**`Access denied for user ...`**
`DB_USERNAME` / `DB_PASSWORD` do not match your local MySQL credentials. Check `.env`, and note that a real OS environment variable takes precedence over `.env`.

**`Sort expression '["string"]: ASC' must only contain property references`**
Swagger's default placeholder for `sort` was submitted literally. Clear the `sort` field or use a valid field name such as `title,asc`.

**`Cannot deserialize value of type java.util.HashSet<Long> from Integer value`**
`categoryId` was sent as a number instead of an array. Use `"categoryId": [1]`.

**`Could not connect to server` when calling the API**
The application is not running, or it failed during startup. Check the terminal running `java -jar ...` for an `ERROR` line.

**Port 8080 already in use**
A previous instance is still running. Find and stop it:

```powershell
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

Or run on a different port: add `server.port=8081` to `application.properties`.

**Changes to `.env` seem to be ignored**
An OS-level environment variable with the same name overrides `.env`. Check with `echo $env:JWT_SECRET` (PowerShell) and remove it with `Remove-Item Env:\JWT_SECRET`.
