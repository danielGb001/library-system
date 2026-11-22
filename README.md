# Library Management System

[![CI](https://github.com/danielGb001/library-system/actions/workflows/ci.yml/badge.svg)](https://github.com/danielGb001/library-system/actions/workflows/ci.yml)

**Live demo:** [Swagger UI on Render](https://library-system-blpx.onrender.com/swagger-ui.html) (free-tier hosting: the first request after a quiet period can take a minute or two, and the free demo database is occasionally powered off by its host, so the demo may be briefly unavailable). To try the write endpoints, click **Authorize** in Swagger UI and use the demo login listed in the [Security](#security) section.

A RESTful library management API built with **Spring Boot**, demonstrating a layered architecture (Controller → Service → Repository → Entity), relational data modeling, business-rule enforcement, and centralized error handling.

Members can borrow and return books, with the system automatically tracking availability, enforcing borrowing limits, and preventing invalid operations (e.g. borrowing a book with zero copies left, or returning a loan twice).

## Tech Stack

- **Java 17**
- **Spring Boot 4** — Web, Data JPA, Validation
- **Hibernate** (JPA implementation)
- **MySQL** — relational database
- **Flyway** — versioned database migrations (the schema lives in `src/main/resources/db/migration`)
- **Lombok** — reduces boilerplate (getters/setters/constructors)
- **Maven** — build and dependency management

## Architecture

The project follows a standard layered architecture:

```
Controller  →  Service  →  Repository  →  Entity
  (REST API)   (business logic)   (data access)   (DB mapping)
```

- **Controllers** handle HTTP requests/responses only — no business logic lives here. They accept request DTOs (validated with `@Valid`) and return response DTOs, so the API contract stays separate from the database entities.
- **Services** contain the actual rules (e.g. "a member can't have more than 3 active loans", "returning an already-returned loan is not allowed").
- **Repositories** use Spring Data JPA — most queries are generated automatically from method names, with a couple of custom `@Query` methods for more complex lookups.
- **A global exception handler** (`@RestControllerAdvice`) converts domain exceptions into consistent, correctly-coded JSON error responses (404, 400, 409) instead of raw stack traces.

## Domain Model

| Entity | Relationship |
|---|---|
| `Author` | has many `Book`s |
| `Book` | belongs to one `Author`, has many `Loan`s over time |
| `Member` | has many `Loan`s |
| `Loan` | links one `Book` to one `Member`, tracks borrow/due/return dates |

All entity IDs are **UUIDs**, generated and mapped explicitly (`@JdbcTypeCode(SqlTypes.CHAR)`) for consistent storage as `CHAR(36)`.

## Business Rules Enforced

- A book can only be borrowed if it has available copies.
- A member cannot exceed **3** simultaneously active loans.
- A loan cannot be returned more than once.
- Borrowing/returning correctly increments and decrements a book's available copy count, wrapped in a single database transaction (`@Transactional`) so partial updates can't occur.
- These rules hold under **simultaneous requests**: borrowing locks the member's row and takes a copy with one atomic SQL update, and returning locks the loan's row, so two people can never borrow the last copy, a member can't exceed the loan limit, and a loan can't be returned twice at the same moment. See [Concurrency](#concurrency).

## Getting Started

### Prerequisites
- Java 17+
- Maven (or use the included `./mvnw` wrapper)
- MySQL running locally

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/danielGb001/library-system.git
   cd library-system
   ```

2. Create a MySQL database:
   ```sql
   CREATE DATABASE library_system;
   ```

3. Add your local database credentials. The committed `application.properties` deliberately contains no password. Spring Boot automatically loads a `config/application.properties` file from the project root and lets it override the main one, and the `config/` folder is git-ignored so your secrets are never committed. Create it:
   ```bash
   mkdir -p config
   ```
   Then put this in `config/application.properties` (the last five lines are the four logins for the two API roles plus a key that signs login tokens; choose your own values, because the app will not start without them; `JWT_SECRET` must be at least 32 characters, for example the output of `openssl rand -base64 48`):
   ```properties
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   ADMIN_USERNAME=YOUR_ADMIN_NAME
   ADMIN_PASSWORD=YOUR_ADMIN_PASSWORD
   USER_USERNAME=YOUR_USER_NAME
   USER_PASSWORD=YOUR_USER_PASSWORD
   JWT_SECRET=YOUR_LONG_RANDOM_SECRET
   ```

4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080`.

On the first run, Flyway creates all tables automatically by applying `src/main/resources/db/migration/V1__init.sql` to the empty database, and Hibernate (`ddl-auto=validate`) then checks that they match the entities. Any later schema change goes into a new numbered migration file (`V2__...`), and a migration that has already been applied is never edited.

### API Documentation (Swagger UI)

Interactive API docs are available once the app is running, at:

```
http://localhost:8080/swagger-ui.html
```

This lets you browse every endpoint, see expected request/response shapes, and send test requests directly from the browser — no Postman required.

Opening the root URL (`http://localhost:8080/`) redirects to the Swagger page. A health check is available at `http://localhost:8080/actuator/health`; it reports `UP` while the application and its database connection are healthy. It is the only Actuator endpoint exposed.

## API Endpoints

Reading is public. Borrowing and returning books needs a login (any role); creating, updating and deleting authors, books and members needs the `ADMIN` role. See [Security](#security).

### Authors
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/authors` | Create an author |
| `GET` | `/api/authors/{id}` | Get an author by ID |
| `GET` | `/api/authors` | List all authors |
| `PUT` | `/api/authors/{id}` | Update an author |
| `DELETE` | `/api/authors/{id}` | Delete an author |

### Books
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/books` | Create a book (requires `authorId` in body) |
| `GET` | `/api/books/{id}` | Get a book by ID |
| `GET` | `/api/books` | List books, one page at a time, with optional title search (see below) |
| `PUT` | `/api/books/{id}` | Update a book |
| `DELETE` | `/api/books/{id}` | Delete a book |

`GET /api/books` returns one page at a time. Optional query parameters: `title` (only books whose title contains the text, ignoring case), `page` (starting at 0, default 0) and `size` (1 to 100, default 20). Books are sorted by title. For example, `GET /api/books?title=fall&page=0&size=10` returns:

```json
{
  "content": [ { "id": "...", "title": "Things Fall Apart", "...": "..." } ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

An invalid `page` or `size` gets **400**.

### Members
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/members` | Register a member |
| `GET` | `/api/members/{id}` | Get a member by ID |
| `GET` | `/api/members` | List all members |
| `DELETE` | `/api/members/{id}` | Delete a member |

### Loans
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/loans/borrow?bookId={id}&memberId={id}` | Borrow a book |
| `POST` | `/api/loans/{loanId}/return` | Return a borrowed book |
| `GET` | `/api/loans` | List all loans |
| `GET` | `/api/loans/{id}` | Get a loan by ID |
| `GET` | `/api/loans/member/{memberId}` | List the active (not yet returned) loans of one member |

## Example: Borrowing a Book

```bash
curl -u "YOUR_USER_NAME:YOUR_USER_PASSWORD" -X POST "http://localhost:8080/api/loans/borrow?bookId=<book-uuid>&memberId=<member-uuid>"
```

**Response (201 Created):**
```json
{
  "id": "2df2ad61-5954-42c8-a009-25c9c85ff9c1",
  "book": { "title": "Things Fall Apart", "availableCopies": 2, ... },
  "member": { "fullName": "Ada Okafor", ... },
  "borrowDate": "2026-09-24",
  "dueDate": "2026-10-08",
  "returnDate": null
}
```

## Error Handling

Errors return consistent JSON with an appropriate HTTP status:

```json
{
  "timestamp": "2026-09-24T10:01:54.878476Z",
  "status": 409,
  "error": "Conflict",
  "message": "This loan has already been returned"
}
```

| Status | Meaning |
|---|---|
| `404` | Resource not found (e.g. invalid `bookId`) |
| `400` | Invalid request data (validation failure, malformed JSON body, invalid or missing parameter) |
| `409` | Request conflicts with current state or existing data (e.g. double-returning a loan, a duplicate email or ISBN, deleting a record that is still referenced) |

## Security

Write endpoints require a login: either HTTP Basic (username and password with every request) or a Bearer token from `POST /api/auth/login`. Reading stays public.

| Who | What they can do |
|---|---|
| Anonymous (no login) | Every `GET` endpoint, Swagger UI and the API docs, the root redirect, `/actuator/health` |
| `USER` | Everything anonymous can, plus borrow and return books (`POST /api/loans/borrow`, `POST /api/loans/{id}/return`) |
| `ADMIN` | Everything, including creating, updating and deleting authors, books and members |

A request with no login (or a wrong password) gets **401**. A valid login without the required role gets **403**.

In Swagger UI, click the **Authorize** button, enter a username and password, and every request you send with **Execute** includes that login. Live demo login (a `USER` account, so it can borrow and return books but not create or delete anything): username `user_demo`, password `demo+123`.

```bash
curl -u "YOUR_ADMIN_NAME:YOUR_ADMIN_PASSWORD" -X POST http://localhost:8080/api/authors \
  -H "Content-Type: application/json" \
  -d '{"name":"Chinua Achebe","nationality":"Nigerian"}'
```

To log in once and use a token instead of sending the password with every request, call `POST /api/auth/login` (the token is valid for 1 hour):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"YOUR_ADMIN_NAME","password":"YOUR_ADMIN_PASSWORD"}'
```

The response contains `accessToken`. Send it as a Bearer token:

```bash
curl -X POST http://localhost:8080/api/authors \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Chinua Achebe","nationality":"Nigerian"}'
```

A wrong username or password gets **401** with the same message in both cases, so the response does not reveal which usernames exist.

How it works:
- The two accounts come from four environment variables or properties (`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `USER_USERNAME`, `USER_PASSWORD`) and are copied into the `app_user` table when the application starts. Tokens are signed with a fifth value, `JWT_SECRET` (at least 32 characters). Nothing secret is stored in the repository, and the application refuses to start if one of these is missing.
- Passwords are stored only as BCrypt hashes, in the `app_user` table (created by the `V2__create_users.sql` migration).
- The API is stateless: there are no sessions or cookies, and the client sends its credentials or token with every request. For that reason CSRF protection is switched off.
- The rules live in one `SecurityFilterChain` (`config/SecurityConfig.java`) and are covered by `SecurityRulesTest` and `AuthControllerTest`.

## Testing

The project has **112 automated tests**: 111 across the two layers below, plus a context-load test that starts the whole application against a real MySQL. They are written with JUnit 5 and Mockito.

### Service layer — 34 unit tests

Each service is tested in isolation: repositories are mocked with Mockito (`@Mock` / `@InjectMocks`), so these tests verify business logic without touching a database.

| Test Class | Tests | Covers |
|---|---|---|
| `AuthorServiceImplTest` | 12 | CRUD operations, blank-field validation, not-found handling |
| `BookServiceImplTest` | 13 | Book creation/updates, paged title search, available-copies recalculation, not-found and validation errors |
| `LoanServiceImplTest` | 9 | Borrowing (availability checks, max active loans, losing the race for the last copy), returning (double-return prevention), not-found handling |

### Controller layer — 77 web-layer tests

These use `@WebMvcTest` and `MockMvc` to exercise the real HTTP layer (URL mapping, status codes, JSON shape, request validation, and `GlobalExceptionHandler`'s error responses) with the service layer mocked via `@MockitoBean`.

| Test Class | Tests | Covers |
|---|---|---|
| `AuthorControllerTest` | 9 | CRUD endpoints, 404 and 400 error mapping |
| `BookControllerTest` | 18 | CRUD endpoints, paging and title search (with `page`/`size` validation), bean-validation failures (`fieldErrors` responses), 409 on reducing copies below those on loan |
| `MemberControllerTest` | 9 | Create/get/list/delete, blank-name and blank/malformed-email validation |
| `LoanControllerTest` | 14 | Borrow/return flows, 409 on no copies / max loans / double-return, missing or malformed query parameters |
| `RootControllerTest` | 1 | The root URL (`/`) redirects to the Swagger UI |
| `GlobalExceptionHandlerTest` | 7 | Error responses shared by every controller: 409 on duplicate email/ISBN and on deleting a record that is still referenced, 400 on malformed JSON, an invalid UUID or a missing parameter, and the validation-error shape |
| `SecurityRulesTest` | 12 | Access rules from `SecurityConfig`: anonymous reads allowed, anonymous writes get 401, a `USER` gets 403 on admin-only actions, `USER` and `ADMIN` may borrow and return |
| `AuthControllerTest` | 7 | Login endpoint (correct password, wrong password, unknown user, blank fields) and that a token is accepted with its role enforced |

### Running the tests

```bash
./mvnw test
```

> **Note:** the default `LibrarySystemApplicationTests` context-load test starts the full application and connects to the MySQL database configured in `application.properties` (plus your `config/application.properties` override), so MySQL must be running (with the `library_system` database created and the four login values and `JWT_SECRET` from Setup step 3 set) for the full suite to pass. The other 111 tests need no database.

## Concurrency

Borrowing and returning are safe under simultaneous requests.

**The problem.** The original `borrowBook` read a book, checked `availableCopies > 0`, then saved a decremented count. Two requests arriving together could both pass the check. In a test where five members raced for a book with a single copy, the original code handed the copy to all five members in 15 of 20 rounds and to two or three members in the other 5 (0 of 20 rounds correct). The book's counter still ended at 0, so nothing looked wrong.

**The fix.**
- `@Transactional` on `borrowBook` and `returnBook`, so a failure part-way through undoes everything.
- Borrowing locks the member's row (`SELECT ... FOR UPDATE`), so one member's simultaneous borrows take turns and the 3-loan limit can't be bypassed.
- A copy is taken with a single atomic update (`UPDATE book SET available_copies = available_copies - 1 WHERE id = ? AND available_copies > 0`), so two requests can never both take the last copy.
- Returning locks the loan's row, so a loan can only be returned once, and the atomic increment can never push the count above `total_copies`.

**How it was verified.** The unit tests cover the logic with mocked repositories, including a test for losing the race for the last copy. The real locking behaviour was checked manually against a local MySQL with scripts that fire simultaneous HTTP requests (the scripts are in the [`scripts/`](scripts) folder and are not part of the automated suite):

| Scenario | Result |
|---|---|
| 5 members race for the only copy, 20 rounds | Exactly one borrower every round (old code: 0 of 20 rounds correct) |
| The same loan is returned 5 times at once, 10 rounds | Exactly one success, and the copy count goes up by exactly one |
| One member borrows 5 different books at once, 10 rounds | Exactly 3 loans every round               |

To run them yourself, start the app against a throwaway local database (the scripts create test books and members), then in another terminal run:

```bash
bash scripts/race-last-copy.sh
bash scripts/race-return-and-limit.sh
```

They always talk to `http://localhost:8080`, so they cannot hit the live demo. A fully correct run ends with every round reported as OK.

## What This Project Demonstrates

- Layered Spring Boot architecture with clear separation of concerns
- JPA entity relationships (`@OneToMany`, `@ManyToOne`) and UUID primary key handling
- Business logic enforcement at the service layer, independent of HTTP concerns
- Concurrency control with transactions, row locks and atomic updates, verified with simultaneous-request tests against a real database
- Centralized exception handling with meaningful HTTP status codes
- Request validation using Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`, etc.), with clear field-level error messages
- Request and response DTOs (e.g. `AuthorRequest`, `BookResponse`, `LoanResponse`) that separate the API contract from the persistence entities, so entities are never returned directly
- Versioned database schema with Flyway migrations, including adopting an existing production database through a baseline
- Two-layer testing with JUnit 5 and Mockito: service-layer unit tests plus `@WebMvcTest` controller tests covering success and failure paths
- Self-documenting API via OpenAPI/Swagger UI

## Troubleshooting Log

Real problems hit while building this project, with root causes and fixes (including the Spring Boot 4 testing-API changes), are documented in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).
