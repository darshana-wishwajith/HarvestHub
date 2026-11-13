# HarvestHub — Development Status

**Last updated:** 2026-11-13 (Day 1 security baseline) · **Audited revision:** `main` @ `512c29b` · **Working branch:** `fix/day1-security` (uncommitted) · **Next milestone:** Final report 2026-11-17, presentation 2026-11-18
Plan and task board: `docs/RELEASE_PLAN.md`. Working rules: `CLAUDE.md`.

Legend: ✅ **verified** = built, ran, or called locally · 🔍 **inspected** = read the code only, not executed · ❓ = not verified

## 1. Overall picture
| App | Size on `main` | Builds | Functional state |
|---|---|---|---|
| Backend | 96 files, 22 controllers | ✅ `mvnw test`: 7/7 pass · ✅ 46/46 security probes (local) | JWT + roles + ownership enforced (Day 1). Business rules still shallow (see §3) |
| Desktop (officer) | 36 files, 7 FXML views | ✅ `mvnw compile` | Login, queue, review, history, profile. Claim/review errors now surfaced. Mock fallbacks remain; no admin monitoring |
| Android | 139 Java files, 94 layouts | ✅ `assembleDebug` + unit tests + `lintDebug` (0 errors, Day 1) | Farmer, buyer and transporter screens wired to API. Hardcoded user IDs, camera permission missing, no video, no geotag upload |

> Work now happens on `fix/day1-security` (branched from `main` @ `512c29b`). `darshana-branch` is stale (9 commits behind `main`).

## 2. Security findings (highest first)
| # | Sev | Finding | Evidence | Status |
|---|---|---|---|---|
| S1 | Critical | Secrets committed on `origin/main`: RSA private key, Mongo URI with credentials, JWT secret, mail app password | `harvesthub-backend/harvesthub_key_pair.pem`, `src/main/resources/application.properties`, `out/production/HarvestHub/application.properties` | Partly fixed: untracked + env-driven config. **Rotation still required (you)**; old values remain in git history |
| S2 | Critical | **No authentication on any endpoint**: `.anyRequest().permitAll()` and the JWT filter is commented out ("Temporarily disabled"); 0 `@PreAuthorize` | `config/SecurityConfig.java:47-55` | ✅ Fixed: JWT filter on, everything authenticated except login/register/reset/OTP/media GET |
| S3 | Critical | Account takeover: the public `GET /api/auth/farmer/{email}/profile` returns the whole `User` entity including `resetPasswordToken`. Calling forgot-password and then reading the token lets anyone reset any password | `controller/AuthController.java` | ✅ Fixed: endpoint removed; reset codes BCrypt-hashed, require email, burned after 5 wrong tries; master passwords `password123`/`admin123` and fuzzy login removed |
| S4 | Critical | Self-registration with any role (`"role":"ADMIN"` accepted). The entity is bound directly to the request | `AuthController.register(@RequestBody User)` | ✅ Fixed: `RegisterRequest` DTO; only FARMER/BUYER/TRANSPORTER |
| S5 | High | Password hashes, NIC and reset tokens returned in API responses; `GET /api/users` lists all users with hashes | `controller/UserController.java`, profile endpoint | ✅ Fixed: `UserDto` everywhere; `/api/users` admin-only |
| S6 | High | Acting user taken from path/body or hardcoded (officer "Lahiru Sahan"; 10 hardcoded IDs in Android) → IDOR on wallets, orders, notifications | `controller/OfficerController.java:125,159`; Android `ui/farmer/{Home,Listings,Wallet,Settings,CreateListing}Fragment.java`, `transporter/DeliveryDashboardActivity.java:55` | ✅ Fixed: `CurrentUser` ownership checks on farmer/wallet/notification/order/chat/offer/wanted-ad endpoints; officer = token user; Android uses `SessionManager.getUserId()` |
| S7 | High | `DataInitializer` overwrites an officer's password with a hardcoded value on every startup, against whatever DB is configured (including Atlas) | `config/DataInitializer.java:53-63` | ✅ Fixed: seeding opt-in (`SEED_DEMO_DATA`), never modifies existing users |
| S8 | Medium | Unauthenticated media upload, no MIME/size check, client-supplied extension kept | `controller/MediaController.java:23-48` | ✅ Fixed: auth + magic-byte type check + size limit + generated names (`MediaStorageService`); profile-photo path traversal fixed too |
| S9 | Medium | Free-form `PUT /orders/{id}/status`: any status can be set by anyone | `controller/OrderController.java`, `service/OrderService.updateOrderStatus` | Partly fixed: parties only, per-role whitelist (farmer: HARVESTING/HARVEST_READY; buyer: LOADING_APPROVED/REJECTED). Full state machine = BE-4 |
| S10 | Medium | JWT has no role claim; CORS `*` | `util/JwtUtil.java`, `SecurityConfig` | ✅ Fixed: role claim, subject = user id, CORS headers restricted, no credentials |
| S11 | Low | Android: client-side SHA-256 "pre-hash" with plaintext fallback; silent fallback to unencrypted prefs; `allowBackup=true`; unused `network_security_config.xml` | `utils/PasswordUtils.java:27`, `utils/SessionManager.java:39`, `AndroidManifest.xml:11` | ✅ Fixed: pre-hash removed (server migrates legacy hashes at login), `allowBackup=false`. Encrypted-prefs fallback remains (low) |
| S12 | Low | Test/debug endpoint `/api/auth/darshana` exposed | `AuthController.java` | ✅ Fixed: removed |

### Found and fixed during Day 1 (not in the Day 0 list)
| # | Sev | Finding | Fix |
|---|---|---|---|
| S13 | Critical | Login accepted master passwords `password123` / `admin123` and plaintext comparison for **any** account; identifier matched by partial name/id | Removed; exact email/mobile/NIC lookup; lockout after 5 failures (15 min) |
| S14 | High | Farmer could create/update a listing with `status: ACTIVE`, bypassing officer verification | Status/grade/owner are server-set; material edits to an ACTIVE listing send it back to PENDING |
| S15 | High | Order creation accepted `status`, `transporterId`, `escrowStatus`, `buyerId` from the client | Server-owned; listing must be ACTIVE; farmer derived from listing |
| S16 | High | Wanted ads: client-supplied `requestId` could overwrite another buyer's ad | Id generated server-side; buyer = token user |
| S17 | Medium | Buyer offer lists fell back to **all** offers when none matched | Returns only own offers |
| S18 | Medium | Officer claim had no lock; any officer could review without claiming | Atomic `findAndModify` claim (409 on conflict); review only by claimant; grade A/B/C mandatory on approve |

## 3. Requirement gap matrix (proposal = authority)
| ID | Requirement (source) | Status | Evidence / gap |
|---|---|---|---|
| FR-AUTH-1 | Register and log in for 4 roles, JWT (§4.1, §6.4.2) | **IMPLEMENTED** ✅ | Verified by probes + `SecurityRulesTests` |
| FR-AUTH-2 | Role-based access (§6.4.2) | **IMPLEMENTED** ✅ | `SecurityConfig` role areas + `CurrentUser` ownership checks |
| FR-LIST-1 | Farmer listing with location-tagged real-time photos (Obj 1, §3.1) | **PARTIAL** | `POST /listings/create` exists; CameraX captures photos, but the **CAMERA permission is missing**, photos are not uploaded (no media call in `ApiService`), and no geotag/EXIF is sent to the backend |
| FR-LIST-2 | 10 s video (§3.1) | **MISSING** | No `VideoCapture`/`Recorder` in Android |
| FR-LIST-3 | Location-locked initiation (Workflow 1.2) | **PARTIAL** | `LocationVerificationActivity` exists; not enforced server-side |
| FR-VER-1 | Shared queue, claim lock, approve with grade / retake / reject (§6.7) | **IMPLEMENTED** (backend ✅ probed; desktop UI 🔍 not run) | Atomic claim, claimant-only review, mandatory grade |
| FR-VER-2 | Media authenticity check: timestamp + GPS vs farm (§3.3.1) | **MISSING** | `gpsCoordinates` is a free string; no check |
| FR-MKT-1 | Buyer search and filter by crop, grade, location, quantity, price (§6.6) | **PARTIAL** | `GET /buyer/listings?search&grade` only |
| FR-MKT-2 | Buyer wanted ads, farmer supply offers (§4.1) | **PARTIAL** | Create and list exist (wanted ads, offers); 🔍 not executed |
| FR-CHAT-1 | In-app negotiation chat (Obj 3) | **PARTIAL** | REST chat endpoints + `ChatActivity`; polling only; ❓ |
| FR-LOG-1 | Automatic vehicle selection by weight (§6.8) | **BROKEN** | Vehicle tier comes from the client; weight is not used |
| FR-LOG-2 | Fee from distance + weight (+ fuel) via GPS / Maps (§6.8) | **BROKEN** | Distance from a hardcoded city-name table (`LogisticsService.estimateDistance`); no weight term; Maps key placeholder |
| FR-ORD-1 | State machine with strict transitions (§6.9) | **PARTIAL** | Transporter events validated; generic status PUT bypasses everything (S9) |
| FR-PAY-1 | Mock escrow advance + 3% buyer fee (§4.1, §6.12) | **PARTIAL** | `recordAdvancePayment` stores the client-sent amount; fee only computed in `LogisticsController` |
| FR-PAY-2 | Release of advance to farmer at loading (Obj 5) | **MISSING** | No wallet credit/debit logic (`WalletService` is read-only) |
| FR-HS-1 | 2-way handshake: transporter + buyer confirm, then auto-complete (§4.1, Obj 6) | **MISSING** | Transporter `DELIVERY_COMPLETED` alone sets `DELIVERED`; no buyer confirmation call in Android |
| FR-PAY-3 | Settlement 3% / 5% / 0%; refunds on rejection (§6.12) | **MISSING** | — |
| FR-PRE-1 | Pre-loading photos + buyer approve/reject (Obj 5) | **PARTIAL** | `LOADING_EVIDENCE_SUBMITTED` event exists; photo upload and buyer approval UI ❓ |
| FR-TRACK-1 | Live tracking for buyer (§4.1) | **MISSING** | Only one-shot location; no periodic updates |
| FR-RATE-1 | Ratings + trust score (§6.10) | **PARTIAL** | `POST /orders/{id}/rate` exists but no Android caller; trust score hardcoded `"4.9"` (`DashboardService:123`) |
| FR-ADM-1 | Admin monitoring of users, orders, disputes (§6.3.1) | **MISSING** | No desktop views |
| NFR-SEC | Secure data handling (§6.4.2) | **PARTIAL** | Day 1 fixes done; secret rotation + redeploy pending |
| NFR-TEST | Testing (§7) | **PARTIAL** | 7 backend tests incl. security rules; Android/desktop still template only |
| NFR-CI | Automated build/deploy | **MISSING** | No CI on any branch; manual `deploy2.ps1` (scp + systemctl) |

## 4. Known issues (non-security)
- **Docs:** README says MySQL 8 and Spring Boot 4; actual stack is MongoDB and Spring Boot 3.2.1.
- **Hardcoded data:** `Listing` stores quantity/price as `String`, so numeric filters and sums are fragile.
- **Silent mock data:** Desktop `DashboardController` and `ReviewHistoryController` load mock data on API failure, which hides real errors.
- **Mock data in Android:** 8 Android files contain mock/dummy data (e.g. `BuyerDemandsFragment`, `BuyerOffersFragment`).
- **Old branch only:** Android crash risks found on the old branch (null `binding` in callbacks, `parseDouble` without validation, `BatchBottomSheetFragment` without a no-arg constructor) are not yet re-checked on `main`.
- **Clutter:**
  - JVM crash logs (`hs_err_pid*`, `replay_pid*`) in `harvesthub-android/`
  - committed `out/` and `.idea/`
  - empty `harvesthub-android/.git` and `.agents/` folders
  - duplicate screens (`MarketInsightsActivity`, `ListingsHistoryActivity`, `FarmerRegisterActivity`, unused ViewModels/Repositories)
- **Branches:** `lahiru-branch` has 1 unmerged commit; there's also the `appmod/java-upgrade` branch and a stash.

### Deployment impact of Day 1 (must do before the new backend goes live)
- The server must provide config as environment variables (systemd `Environment=` or an `EnvironmentFile`): `MONGODB_URI`, `MONGODB_DATABASE`, `JWT_SECRET` (new, ≥32 bytes), `MAIL_USERNAME`, `MAIL_PASSWORD`, optionally `GOOGLE_MAPS_API_KEY`, `SEED_DEMO_DATA`, `DEMO_PASSWORD`. Without `JWT_SECRET` the app refuses to start (intended).
- Old Android/desktop builds will get 401/403 on most calls against the new backend; ship the new APK and desktop build together with it.
- Changing `JWT_SECRET` logs everyone out once (expected).

## 5. Open questions (proposal conflicts)
1. Delivery confirmation: §4.1 and Obj 6 say 2-way; Fig. 1 says 3-way. → Plan: **2-way** (proposal text).
2. Roles: §6.4.2 lists Farmer/Buyer/Transport/Admin; §6.5 lists Farmer/Buyer/Driver/Officer. → Plan: FARMER, BUYER, TRANSPORTER, OFFICER, ADMIN.
3. Rating: §6.10 says "users rate each other" and §4.1 says farmers and buyers. → Plan: farmer and buyer rate each other; the transporter is rated by the buyer (Tier 2).
4. Advance amount: not defined in the proposal; the workflow implies 20% crop plus transport plus 3%. → Needs your confirmation.
5. Transport helper: separate account or the transporter's own app? → Plan: the transporter's app (no extra role).

## 6. Verification log
| Date | Command | Result |
|---|---|---|
| 13 Nov | backend `mvnw test` on `main` (local Mongo `hh_audit`) | ✅ 2 tests pass |
| 13 Nov | backend `java -jar app.jar` on :18080 + curl probes (local only) | ✅ S2–S5 reproduced |
| 13 Nov | desktop `mvnw compile` on `main` | ✅ |
| 13 Nov | Android `assembleDebug lintDebug testDebugUnitTest` on `main` | ✅ debug APK + unit tests pass · ❌ `lintDebug` fails: **31 errors** (29 `InvalidVectorPath`: `.01`-style numbers in `ic_device_handshake`, `ic_dock_terminal`, `ic_tag`, `transporter_ic_call` and others, which crash on some devices; 2 `NotSibling` in `activity_negotiation_chat.xml:29,31`) and 2,550 warnings (1,127 HardcodedText, 524 UnusedResources) |
| 13 Nov (Day 1) | backend `mvnw test` 7/7; 46/46 local security probes; desktop compile; Android `assembleDebug lintDebug testDebugUnitTest` | ✅ all green (lint 0 errors / 2,558 warnings) |
| 13 Nov | Android build on `darshana-branch` | ❌ 16 missing color resources (`fragment_home.xml`, `fragment_create_listing.xml`); the branch is stale, so not a `main` issue |

## 7. Task board
Current task: Day 1 complete on `fix/day1-security` (uncommitted), waiting for review/commit. Next: Day 2 core logic.
| Phase | Done | Total |
|---|---|---|
| Day 1 security baseline (SEC-1..9, BLD-1) | 9 (SEC-1 rotation is yours) | 10 |
| Day 2 core logic (BE-1..6, AN-1..3, DT-1) | 0 | 10 |
| Day 3 polish/QA (UI-1..2, QA-1..3, BLD-2) | 0 | 6 |
