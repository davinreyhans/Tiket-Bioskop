# Tiket-Bioskop

Tiket Bioskop is a program I created to implement CRUD using Java Spring Boot.

## Setup

Requires JDK 17+ and a Supabase (PostgreSQL) project.

1. `cp .env.example .env` and fill in the Supabase session pooler credentials and a `JWT_SECRET`.
2. `mvn spring-boot:run` — Flyway creates the tables in schema `bioskop` on first start.
3. Open Swagger UI at http://localhost:8080/swagger-ui.html

Tests run against an in-memory H2 database, no Supabase needed: `mvn test`.

## Auth

JWT. Register with `POST /users`, log in with `POST /auth/login` (`{"username":"…","password":"…"}`),
then send `Authorization: Bearer <token>` on every request. A token is valid for 24 hours; there is no refresh
token, so log in again after it expires (or after changing your username).
In Swagger UI use the **Authorize** button. New users get role `USER`. To make an admin, promote them in the Supabase SQL editor:

```sql
update bioskop.users set role = 'ADMIN' where username = '<username>';
```

## Endpoints

| Method | Path | Access |
|---|---|---|
| GET | `/films`, `/films?showing=true`, `/films/search?name=`, `/films/{id}` | public |
| POST / PUT / DELETE | `/films`, `/films/{id}` | admin |
| POST | `/auth/login` | public |
| GET | `/schedules` (optional `?filmId=&date=YYYY-MM-DD`), `/schedules/{id}`, `/schedules/{id}/seats` (free seats) | public |
| POST / PUT / DELETE | `/schedules`, `/schedules/{id}` (delete only when no tickets are booked) | admin |
| POST | `/users` (register) | public |
| GET / PUT | `/users/me` | logged in |
| GET / DELETE | `/users`, `/users/{username}`, `/users/{id}` | admin |
| POST | `/tickets` (`{"scheduleId":1,"seatsCodes":["A1","A2"]}`, max 10 seats, all or nothing, until the show starts) | logged in |
| DELETE | `/tickets/{id}` (owner or admin, up to 2 hours before the show) | logged in |
| GET | `/tickets/me` | logged in |

List endpoints (`/films`, `/films/search`, `/schedules`, `/users`) are paged: `?page=0&size=20&sort=field,asc`
(size max 100). The response is `{"content": [...], "page": {"size", "number", "totalElements", "totalPages"}}`.

Seats are seeded by the first migration: studios `A`–`C`, seats `A1`–`E10`.

Schedules: an end time before the start time means the show ends the next day (`23:00`–`01:00`); a show lasts at most 6 hours,
and two shows can't overlap in the same studio.
