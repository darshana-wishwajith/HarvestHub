# HarvestHub — Release Plan (13 → 17 Nov 2026)

Evidence for every item is in `DEVELOPMENT_STATUS.md`. IDs: `SEC-*` security, `BLD-*` build/baseline, `BE-*` backend, `DT-*` desktop, `AN-*` Android, `UI-*` design, `QA-*` tests/CI.
**Status: proposed. Nothing below is started until approved.**

## Guiding decisions
- Base all work on **`main`** (512c29b). `darshana-branch` is 9 commits behind and does not contain the officer portal, buyer/transporter apps, or most backend services.
- Security first: the deployed API is currently fully open (no authentication). Fixing it touches every client call, so it must land **before** feature work, not after.
- Demo flow = Tier 1 below. Anything not finished by **16 Nov 20:00** moves to "Future work" in the report.

## Tier 1 — must work end-to-end on 17 Nov
Register/login per role → farmer creates listing with geotagged camera photos → officer claims, grades, approves (JavaFX) → buyer searches/filters and orders (or farmer answers a wanted ad) → system picks vehicle + computes cost → buyer pays mock advance (+3%) → transporter accepts, uploads pre-loading photos, buyer approves → start journey (location updates visible to buyer) → transporter "Delivery completed" + buyer "Goods received" → automatic settlement (3% / 5% / 0%) → ratings update trust score.

## Tier 2 — if time allows
Pre-loading and post-delivery rejection with refunds and penalties (workflow §5.3), 10-second video capture, in-app chat polish, admin screens (users/orders/disputes), market insights from real data.

## Tier 3 — document as future work
Google Maps Distance Matrix (unless a key is available; the fallback is straight-line distance from GPS), continuous background tracking, return-journey logistics, automated dispatch retry loop, AI grading (already out of scope).

## Day 1 — Fri 14 Nov: secure baseline
| ID | Task | Files | Done when |
|---|---|---|---|
| SEC-1 | **(You)** Rotate EC2 key pair, Atlas DB user password, Gmail app password, JWT secret | — | Old values no longer work |
| SEC-2 | Untrack `harvesthub_key_pair.pem`, `application.properties`, `out/`; read config from env vars + gitignored `application-local.properties`; add `application.properties.example` | backend `src/main/resources`, `.gitignore`, `deploy2.ps1` | `git ls-files` shows no secrets; backend starts from env |
| SEC-3 | Re-enable `JwtRequestFilter`; put role in the JWT; `SecurityConfig` rules per role (`/api/auth/**` public, officer endpoints OFFICER/ADMIN, etc.) | `config/SecurityConfig.java`, `util/JwtUtil.java`, `util/JwtRequestFilter.java` | Unauthenticated calls → 401; wrong role → 403 |
| SEC-4 | Registration via DTO; client may only choose FARMER/BUYER/TRANSPORTER; officers/admins are seeded | `controller/AuthController.java`, `service/UserService.java` | `role:"ADMIN"` registration rejected |
| SEC-5 | Delete the public `GET /api/auth/farmer/{id}/profile` and `/darshana`; make `UserController` admin-only and return DTOs | `controller/AuthController.java`, `controller/UserController.java` | No response contains `password` or `resetPasswordToken` |
| SEC-6 | Get the acting user from the token. Replace the hardcoded officer "Lahiru Sahan" (backend) and 10 hardcoded user IDs (Android) | `controller/OfficerController.java`, Android `ui/farmer/*`, `ui/transporter/DeliveryDashboardActivity.java` | Two officers or two farmers see only their own data |
| SEC-7 | Single authenticated Retrofit client (interceptor everywhere); handle 401 by logging out | Android `network/ApiClient.java` + callers | Every protected call sends the Bearer token |
| SEC-8 | Stop `DataInitializer` resetting real passwords; move demo accounts to a `local`/`demo` profile seed | `config/DataInitializer.java`, `mongo-init/` | Startup never changes existing users |
| SEC-9 | Media upload: auth required, image/video MIME + size whitelist, server-chosen extension | `controller/MediaController.java` | `.html`/oversized uploads rejected |
| BLD-1 | All 3 apps build on a clean checkout of `main` (done for backend/desktop; Android: fix lint errors, add CAMERA permission) | Android `AndroidManifest.xml` | `assembleDebug lintDebug` green |

## Day 2 — Sat 15 Nov: core business logic (backend first, then clients)
| ID | Task | Proposal ref |
|---|---|---|
| BE-1 | Atomic officer claim (only PENDING and unclaimed); review only by the claimant; listing status enum | §6.7 |
| BE-2 | Listing create stores GPS, media URLs and capture timestamps; officer sees them; authenticity check (distance from farm, age) | §3.3.1, Obj 1 |
| BE-3 | Vehicle chosen automatically by weight; distance from farmer/buyer GPS (haversine, or Maps if a key exists); fee = distance×rate + weight×handling | §6.8 |
| BE-4 | Order state machine with role-checked transitions, removing free-form `PUT /orders/{id}/status` | §6.9 |
| BE-5 | Mock wallet: server computes advance + 3% fee, escrow hold, release of crop portion on START_JOURNEY, 2-way handshake (transporter + buyer), settlement 3/5/0, transaction records | §4.1, §6.12 |
| BE-6 | Ratings per party; trust score = average rating + completion reliability (replacing the hardcoded `"4.9"`) | §6.10 |
| AN-1 | Farmer: camera returns photos to create-listing; geotag + upload via `/api/media/upload`; no gallery path | Obj 1 |
| AN-2 | Buyer: "Goods received" confirmation, pre-loading approve/reject, rating screen | Obj 5, 6 |
| AN-3 | Transporter: periodic location post while IN_TRANSIT; buyer tracking screen polls it | §4.1 Live Tracking |
| DT-1 | Officer portal against secured API; remove mock fallbacks (show error state instead) | §6.3.1 |

## Day 3 — Sun 16 Nov: polish, UI, tests, CI
| ID | Task |
|---|---|
| UI-1 | Screen-by-screen comparison with `docs/figma/` exports; fix the highest-visibility screens of the demo flow first |
| UI-2 | Remove every mock/dummy fallback from the demo path (8 Android files, 2 desktop controllers); add empty/error states |
| QA-1 | Backend tests: auth/RBAC, state machine, fee calculation, claim lock (MockMvc + local Mongo) |
| QA-2 | GitHub Actions: backend `mvnw test`, desktop `mvnw package`, Android `assembleDebug lintDebug` on PRs to `main` |
| QA-3 | Scripted end-to-end walkthrough of Tier 1 on local DB; fix blockers |
| BLD-2 | Fix README (MongoDB, Spring Boot 3.2), remove crash logs / dead duplicates listed in status |
| — | Tier 2 items as time permits |

## Mon 17 Nov — freeze
Morning: deploy backend (after SEC-1/2), run the walkthrough against the deployed server, build the release APK and desktop package, take report screenshots. No new features.

## Team split (6 people)
Recommended split after Day 1: one owner per row so two people never touch the same files.
- **Backend (2):** BE-1/2/3 and BE-4/5/6
- **Android (2):** farmer (AN-1) and buyer/transporter (AN-2/3)
- **Desktop (1):** DT-1, UI-1 desktop
- **QA/report (1):** QA-3, screenshots, report

## Approval needed
1. Base on `main` and the Tier 1/2/3 cut.
2. SEC-2 history scrub: rotating the secrets is enough if the repo is private. Rewriting history needs a force-push the whole team must re-clone after. Recommend: rotate only, no rewrite.
3. Merge or drop the 1 unmerged commit on `lahiru-branch` and the `appmod/java-upgrade` branch/stash.
