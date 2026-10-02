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
- Tests: MockMvc renders no error body, assert messages via `result.getResolvedException()`

## Spring Boot 4 gotchas (silently ignored, no error)
- Error body fields: `spring.web.error.include-*`, not `server.error.include-*`
- Pageable sort: `@SortDefault`, not `@PageableDefault` (its `size=10` overrides `spring.data.web.pageable.default-page-size`)
- Starters were renamed: `spring-boot-starter-webmvc`, `-security-oauth2-resource-server`, `-flyway`, `-webmvc-test`

## Frontend
- All HTTP via `api()` / `useApi()` in `src/api.ts` (adds Bearer token, 401 → `/login?expired=1`); paths start with `/api` only through `BASE_URL`
- List endpoints return `{ content, page: { size, number, totalElements, totalPages } }`; error bodies have `message` and `errors[]` for 400

## Workflow
- After finishing a task, tick it `[x]` in `TODO.md` and update README endpoints if the API changed
