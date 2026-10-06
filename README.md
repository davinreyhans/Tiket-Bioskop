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

Frontend and backend deploy together as one Vercel project, using
[Vercel Services](https://vercel.com/docs/services) (`vercel.json`):

| Path | Service | Built from |
|---|---|---|
| `/api/*` | `backend` | `Dockerfile` (Spring Boot, served under `/api`, port 80) |
| everything else | `frontend` | `frontend/` (Vite build, SPA fallback to `index.html`) |

Both are on the same domain, so the frontend calls `/api/...` with no CORS and no `VITE_API_URL`.
CI (`.github/workflows/ci.yml`) runs the backend tests, builds the Docker image and lints + builds the frontend.

1. Import the repo in Vercel (Hobby is enough). Keep the *Root Directory* at the repo root.
2. Set the environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (a new random one,
   not your local secret).
3. Set the Functions region close to your Supabase region (Settings → Functions), e.g. `sin1` for Singapore.
4. Deploy. Flyway creates the tables on the first start; then make the first admin with the SQL below.

Free tier notes: the backend scales to zero after 5 minutes without traffic, so the next request waits for
Spring Boot to start; a free Supabase project pauses after a week without activity.

The same `Dockerfile` runs on other hosts (Render, Fly.io, ...): they set `PORT`, and the frontend then needs
`VITE_API_URL=https://<backend-host>/api` plus `CORS_ALLOWED_ORIGINS=<frontend URL>` on the backend.

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
