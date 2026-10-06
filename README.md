# Tiket-Bioskop

Tiket Bioskop is a program I created to implement CRUD using Java Spring Boot.

## Setup

Requires JDK 17+ and a Supabase (PostgreSQL) project.

1. `cp .env.example .env` and fill in the Supabase session pooler credentials and a `JWT_SECRET`.
2. `mvn spring-boot:run` — Flyway creates the tables in schema `bioskop` on first start.
3. Open Swagger UI at http://localhost:8080/swagger-ui.html

Tests run against an in-memory H2 database, no Supabase needed: `mvn test`.

### Frontend

Vite + React + TypeScript in `frontend/` (Node 20.19+ / 22.12+):

```sh
cd frontend
npm install
npm run dev   # http://localhost:5173
```

With the backend running on :8080, call it from the frontend as `/api/...` (e.g. `/api/films`):
the Vite dev server proxies it to `http://localhost:8080/films`, so no CORS setup is needed in development.

## Deploy

Backend and frontend deploy separately; CI (`.github/workflows/ci.yml`) runs the backend tests and the
frontend lint + build on every push.

**Backend** (any host that runs Java 17: Render, Railway, Fly.io, a VPS, ...):

- build: `mvn -B package -DskipTests`, run: `java -jar target/tiket-bioskop-0.0.1-SNAPSHOT.jar`
- env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`
  (the frontend URL, e.g. `https://tiket-bioskop.vercel.app`); `PORT` is honored when the host sets it
- Flyway creates or updates the tables on startup

**Frontend** (Vercel): import the repo, set *Root Directory* to `frontend`, and set
`VITE_API_URL` to the backend URL (e.g. `https://tiket-bioskop-api.example.com`). `frontend/vercel.json`
sends every path to `index.html`, so refreshing a page like `/films/1` works.

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
