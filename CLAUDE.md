# HarvestHub — working notes for Claude

Agricultural marketplace + logistics system (ICT 2212, Group 12 "CodeX", Rajarata University).
Deadlines: **Final report 2026-11-17, presentation 2026-11-18.**
Status, gaps and the task board live in `DEVELOPMENT_STATUS.md`; the triaged plan is in `docs/RELEASE_PLAN.md`. Read those before starting work; don't duplicate them here.

## Scope authority
1. Approved proposal (`Project Proposal - Revised.pdf`, kept outside the repo) **wins**.
2. `Project Workflow - Updated (Final).pdf` fills gaps only where it does not contradict the proposal.
3. Existing code is **not** a requirements source. Don't invent features. Open conflicts are listed in `DEVELOPMENT_STATUS.md` → "Open questions".

## Repo layout (monorepo, work from `main`)
| Path | What | Stack |
|---|---|---|
| `harvesthub-backend/` | REST API | Spring Boot 3.2.1, Java 21 target, Spring Data MongoDB, Spring Security + JJWT 0.11.5, Lombok 1.18.48 |
| `harvesthub-desktop/` | Agricultural Officer / Admin portal | JavaFX 21.0.6, `java.net.http` + Jackson, FXML + `styles.css` |
| `harvesthub-android/` | Farmer, Buyer, Transporter app | Java 8 source, XML layouts, AGP 8.13.2 / Gradle 8.14.5, compileSdk 34 / minSdk 21, Retrofit 2.9.0, CameraX 1.3.1 |
| `mongo-init/` | Mongo seed script + data | `seed_harvesthub_database.js` |

Backend packages: `controller/ service/ repository/ model/ dto/ config/ util/ exception/` under `com.harvesthub.backend`.
Android packages: `ui/{farmer,buyer,transporter,common}`, `network/ApiService.java` (all Retrofit endpoints), `network/ApiClient.java`, `utils/SessionManager.java`, `model/`.
Desktop: controllers in `com.harvesthub.desktop`, HTTP in `api/ApiClient.java`, session in `api/Session.java`.

## Run commands (when the user says "run backend / run desktop / run mobile app")
Run from the repo root in PowerShell; backend first, each in its own terminal:
| Say | Command | Does |
|---|---|---|
| run backend | `powershell -File scripts\run-backend.ps1` | Spring Boot on :8080 using `application-local.properties` (Atlas) |
| run desktop | `powershell -File scripts\run-desktop.ps1` | JavaFX portal → `http://localhost:8080` (`-Production` → harvesthubapp.tech) |
| run mobile app | `powershell -File scripts\run-mobile.ps1` | Builds debug APK pointed at `http://<laptop Wi-Fi IP>:8080/`, installs over USB with adb (`D:\Android\platform-tools`), launches it (`-Production` → harvesthubapp.tech) |
Android debug builds take the API URL from `-PapiUrl` (`app/build.gradle` → `BuildConfig.API_BASE_URL`) and allow cleartext HTTP (`src/debug/AndroidManifest.xml`); release builds are HTTPS-only to production.

## Commands (Windows; JDK 25/26 installed, no JDK 21 — builds still work)
```bash
# local MongoDB 8.3 runs as a Windows service on localhost:27017
cd harvesthub-backend && ./mvnw.cmd test            # tests use src/test/resources, no DB writes
cd harvesthub-backend && ./mvnw.cmd spring-boot:run # reads ./application-local.properties
cd harvesthub-desktop && HARVESTHUB_BACKEND_URL=http://localhost:8080 ./mvnw.cmd javafx:run
cd harvesthub-android && ./gradlew.bat assembleDebug lintDebug testDebugUnitTest
```
- Backend config is env-driven (`application.properties` has no secrets). Local dev: copy `harvesthub-backend/application-local.properties.example` → `application-local.properties` (git-ignored). Required: `JWT_SECRET` (≥32 bytes). Production: set the same keys as environment variables on the server.
- `SEED_DEMO_DATA=true` + `DEMO_PASSWORD` create demo accounts `farmer@ / buyer@ / transporter@ / officer@ / admin@harvesthub.lk` (never in production unless intended; existing users are never modified).
- `harvesthub-android/local.properties` (sdk.dir) is untracked and machine-specific.

## Security model (backend)
- `SecurityConfig`: everything requires a JWT except `POST /api/auth/{login,register,forgot-password,reset-password}`, `/auth/*-otp`, `GET /uploads/*`. Officer area (`/api/queue`, `/api/listings/**`, `/api/review-history`, `/api/dashboard/{stats,activity}`, `/api/officers/**`) needs OFFICER/ADMIN; `/api/users` needs ADMIN.
- JWT subject = user id; the filter reloads the user from Mongo each request.
- In controllers use `security/CurrentUser`: `id()`, `requireSelfOrStaff(userId)`, `requireParticipantOrStaff(...)`, `requireRole(...)`.
- Uploads only via `service/MediaStorageService` (magic-byte type check, size limits, generated names).
- Android: one shared authenticated client (`network/ApiClient`, initialised in `HarvestHubApp`); use `SessionManager.getUserId()`; never hardcode user ids; passwords are sent as typed (server hashes).

## Rules
- **Never commit secrets** (PEM keys, Mongo URI with credentials, JWT secret, mail password, Maps key). Config comes from env vars / untracked `application-local.properties`. Never print secret values in output or docs.
- The dev backend uses the team's **MongoDB Atlas** database (user's choice, 2026-11-13), configured in `application-local.properties`. Treat it as shared live data: no destructive operations, no bulk test data, no probes that create accounts/listings unless the user asks. Automated tests use `src/test/resources` and never touch Atlas. Never run `deploy*.ps1` unless the user asks.
- Every non-auth endpoint must require a valid JWT and the right role; derive the acting user from the token, never from a path/body `userId`.
- Return DTOs, never `@Document` entities (no password hashes / reset tokens in responses).
- Order status changes go through the state machine in `OrderService`; no free-form `PUT /orders/{id}/status` for clients.
- Money: mock wallet only (no real gateway — proposal §4.2). Fees: buyer 3%, transporter 5%, farmer 0%; full refund incl. fee on rejection.
- Keep Android/desktop visuals on brand: `#1B4332` (primary dark), `#4CAF50` (accent), `#E8F5EA` (surface), Plus Jakarta Sans. Match Figma exports in `docs/figma/` when present; no redesigns without approval.
- One feature at a time: backend endpoint + test → client wiring → local run → update `DEVELOPMENT_STATUS.md`.
- Don't commit, push, or merge unless the user asks. Work on feature branches off `main`.
