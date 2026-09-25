# Library Management System

A RESTful library management API built with **Spring Boot**, demonstrating a layered architecture (Controller → Service → Repository → Entity), relational data modeling, business-rule enforcement, and centralized error handling.

Members can borrow and return books, with the system automatically tracking availability, enforcing borrowing limits, and preventing invalid operations (e.g. borrowing a book with zero copies left, or returning a loan twice).

## Tech Stack

- **Java 17**
- **Spring Boot 3** — Web, Data JPA, Validation
- **Hibernate** (JPA implementation)
- **MySQL** — relational database
- **Lombok** — reduces boilerplate (getters/setters/constructors)
- **Maven** — build and dependency management

## Architecture

The project follows a standard layered architecture:

```
Controller  →  Service  →  Repository  →  Entity
  (REST API)   (business logic)   (data access)   (DB mapping)
```

- **Controllers** handle HTTP requests/responses only — no business logic lives here.
- **Services** contain the actual rules (e.g. "a member can't have more than 5 active loans", "returning an already-returned loan is not allowed").
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
- A member cannot exceed a maximum number of simultaneously active loans.
- A loan cannot be returned more than once.
- Borrowing/returning correctly increments and decrements a book's available copy count, wrapped in a single database transaction (`@Transactional`) so partial updates can't occur.

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

3. Configure your database credentials in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/library_system
   spring.datasource.username=YOUR_USERNAME
   spring.datasource.password=YOUR_PASSWORD
   ```

4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080`.

## API Endpoints

### Authors
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/authors` | Create an author |
| `GET` | `/api/authors/{id}` | Get an author by ID |
| `GET` | `/api/authors` | List all authors |

### Books
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/books` | Create a book (requires `authorId` in body) |
| `GET` | `/api/books/{id}` | Get a book by ID |
| `GET` | `/api/books` | List all books |

### Members
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/members` | Register a member |
| `GET` | `/api/members/{id}` | Get a member by ID |
| `GET` | `/api/members` | List all members |

### Loans
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/loans/borrow?bookId={id}&memberId={id}` | Borrow a book |
| `POST` | `/api/loans/{loanId}/return` | Return a borrowed book |
| `GET` | `/api/loans` | List all loans |

## Example: Borrowing a Book

```bash
curl -X POST "http://localhost:8080/api/loans/borrow?bookId=<book-uuid>&memberId=<member-uuid>"
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
| `400` | Invalid request data (validation failure) |
| `409` | Request conflicts with current resource state (e.g. double-returning a loan) |

## What This Project Demonstrates

- Layered Spring Boot architecture with clear separation of concerns
- JPA entity relationships (`@OneToMany`, `@ManyToOne`) and UUID primary key handling
- Business logic enforcement at the service layer, independent of HTTP concerns
- Centralized exception handling with meaningful HTTP status codes
- Request validation using Bean Validation (`@Valid`, `@NotBlank`, `@Email`, etc.) with DTOs to separate API contracts from persistence models
