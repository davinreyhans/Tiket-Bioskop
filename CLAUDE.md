# Tiket-Bioskop

Spring Boot 4.1 (Java 17) backend + Vite/React/TS frontend in `frontend/`. Supabase Postgres, schema `bioskop` (not `public`).

## Commands
- `mvn test` - integration tests (MockMvc) on in-memory H2, profile `test`; no Supabase needed
- `cd frontend && npm run build && npm run lint` - type-check + build + oxlint
- `cd frontend && npm run dev` - :5173, proxies `/api/*` → `localhost:8080/*` (prefix stripped)
- Run backend without Supabase: `mvn spring-boot:run -Dspring-boot.run.useTestClasspath=true "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:mem:bioskop;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE --spring.datasource.username=sa --spring.datasource.password="` (profile `test` alone still picks up `.env`)
- `java` on PATH is 1.8; use `$JAVA_HOME/bin/java` (JDK 17). Maven already uses JAVA_HOME

## Backend conventions
- Layers: `controller/` → `usecase/<area>/` (`@Service` + `@RequiredArgsConstructor`, request DTOs as records next to it) → `repository/Dao*`
- Errors: throw `NotFoundException` / `ConflictException` with a clear message; keep the DB constraint as the race backstop
- Schema changes = new Flyway file `src/main/resources/db/migration/V{n}__*.sql`; `ddl-auto=validate`, never let Hibernate create tables
- Schedule times are local cinema time (`bioskop.timezone`); end ≤ start means the show ends next day → use `Schedules.startsAt()/endsAt()`
- H2 hides PostgreSQL typing errors: a nullable param in `:p is null` needs `cast(:p as LocalDate)` etc. (Integer is fine)
- Tests: MockMvc renders no error body, assert messages via `result.getResolvedException()`

## Spring Boot 4 gotchas (silently ignored, no error)
- Error body fields: `spring.web.error.include-*`, not `server.error.include-*`
- Pageable sort: `@SortDefault`, not `@PageableDefault` (its `size=10` overrides `spring.data.web.pageable.default-page-size`)
- Starters were renamed: `spring-boot-starter-webmvc`, `-security-oauth2-resource-server`, `-flyway`, `-webmvc-test`

## Deploy
- One Vercel project via Services (`vercel.json`): `/api/*` → `Dockerfile` backend, rest → `frontend/`
- The Dockerfile sets `SERVER_SERVLET_CONTEXT_PATH=/api` and `PORT=80`; locally there is no prefix (Vite proxy strips it)
- The Dockerfile also sets `SERVER_FORWARD_HEADERS_STRATEGY=framework`: without it every browser POST/PUT/DELETE on Vercel
  gets 403 "Invalid CORS request" (Spring sees http://…:80, not the https origin). curl without `Origin` won't show it
- Vercel kills a container that isn't listening within ~28.6 s; backend region must match Supabase (`sin1`) to start in time
- Docker isn't installed locally: the image is only built in CI (`docker` job)

## Frontend
- All HTTP via `api()` / `useApi()` in `src/api.ts` (adds Bearer token, 401 → `/login?expired=1`); paths start with `/api` only through `BASE_URL`
- List endpoints return `{ content, page: { size, number, totalElements, totalPages } }`; error bodies have `message` and `errors[]` for 400

## Workflow
- After finishing a task, tick it `[x]` in `TODO.md` and update README endpoints if the API changed
