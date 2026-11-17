# HarvestHub — Project Handover & Progress Report

> **Read this first if you are an AI assistant or a new developer taking over.** It explains what HarvestHub is, what the proposal requires, how the system is built, how every part works together, what is done, what is missing, and exactly how to continue safely.
>
> **Written:** 2026-11-17 (evening) by Claude (Anthropic), acting as the project's senior full-stack engineer / architect since 13 Nov 2026.
> **Repo:** `https://github.com/darshana-wishwajith/HarvestHub` · **Working branch:** `fix/day1-security` (⚠️ ~150 changed files **not committed** — see §11.1).
> **Deadlines:** Final report 2026-11-17 · **Final presentation 2026-11-18**.
> **Course:** ICT 2212 (Software Development Project II), Group 12 "CodeX", Rajarata University of Sri Lanka.

**Document map:** §1 summary · §2 problem/aim/objectives · §3 scope · §4 requirements (FR + NFR) with status · §5 architecture · §6 components · §7 end-to-end workflow, state machine & data flow · §8 money, logistics & trust rules · §9 UI inventory · §10 current progress · §11 gaps & risks · §12 how to continue (setup, commands, deploy, test accounts) · §13 engineering approach & rules · §14 decision log · §15 recommended next steps · §16 change log of this file.

Other docs (more detail, older): `CLAUDE.md` (working rules — **mandatory**), `DEVELOPMENT_STATUS.md` (verification log §6 is accurate; its §3 gap matrix is **outdated**, use §4 of this file instead), `docs/FARMER_AUDIT.md`, `docs/BUYER_AUDIT.md`, `docs/TRANSPORTER_AUDIT.md`, `docs/DESKTOP_AUDIT.md` (per-role screen audits + user decisions), `docs/TESTING_REPORT.md`, `docs/report/*.md` (report chapters: architecture, database, implementation), `docs/Project Proposal  - Revised.pdf` and `docs/Project Workflow - Updated (Final).pdf` (**the requirement authorities**).

---

## 1. One-paragraph summary

HarvestHub is a **trusted agricultural marketplace + logistics ecosystem** for the North Central Province of Sri Lanka. **Farmers** list harvests with GPS-locked, geotagged photos and a 10 s video; **Agricultural Officers** verify and grade (A/B/C) every listing on a **JavaFX desktop portal** before buyers can see it; **Buyers** search verified listings or post "wanted ads", negotiate in chat, and pay an **advance into a mock escrow**; the system **automatically picks the vehicle class, calculates the transport fee and dispatches the nearest online driver**; the **Transporter** records pre-loading photo evidence that the buyer approves remotely, shares **live GPS** in transit, and a **2-way handshake** (driver "Delivery Completed" + buyer "Goods Received") settles all money automatically. Disputes (pre-loading or post-delivery rejection) move money automatically per the workflow; officers handle issue tickets/appeals. Everything runs on one **Spring Boot REST API** (deployed at **https://harvesthubapp.tech**, AWS EC2) backed by **MongoDB Atlas**, with an **Android app** (Farmer/Buyer/Transporter) and the **desktop officer portal**.

**State today:** all six proposal objectives are implemented end-to-end and deployed; the full order lifecycle has been run live (purchase → payment → dispatch → loading approval → transit → handshake → settlement → rating, plus both rejection paths). The latest work (17 Nov) was Figma visual alignment of the buyer and transporter apps and a whole-app data/consistency audit. Main risks: **nothing committed to git**, last APK not installed on a phone, UI passes not yet eyeballed on a device.

---

## 2. Problem, aim and objectives (from the proposal)

**Problems (§2):** (1) information asymmetry — farmers don't know demand; (2) trust gap — distant buyers can't verify quality, so middlemen win; (3) logistics inefficiency — small farmers can't find/price transport, produce spoils; (4) financial risk — no safe way to handle advance payments between strangers.

**Novelty (§3):** built-in logistics coordination (vehicle auto-assigned by weight), officer-verified quality (photos/video graded by Dept. of Agriculture officers before going live), end-to-end tracking farm gate → buyer.

**Aim (§5.1):** a trusted marketplace + logistics ecosystem connecting farmers directly with buyers, with officer-verified quality, automated transport costing and a secure payment-first workflow.

**Objectives (§5.2) and status:**

| # | Objective | Status |
|---|---|---|
| O1 | GPS-verified crop listing module (market demand view + listings with real-time, location-tagged media) | ✅ Done — location lock (500 m from registered farm), in-app CameraX capture only (wide photo, close-up, 10 s video), GPS in EXIF/metadata, server-side authenticity checks, Market tab with real prices |
| O2 | Officer verification dashboard (desktop, review/grade/approve photos + GPS) | ✅ Done — JavaFX portal: shared queue, atomic claim lock, media viewer with zoom/video, GPS/time checks, grade A/B/C, retake/reject, history, users, orders, disputes |
| O3 | Marketplace negotiation system (chat, price, pickup date) | ✅ Done — REST chat (polling, not websockets), counter-offers on quantity/price/date, both parties confirm same terms |
| O4 | Automated logistics & dispatch engine (cost by weight + distance, vehicle selection, advance payment, dispatch) | ✅ Done — smallest fitting vehicle, Google Distance (server) with ×1.3 straight-line fallback, fee = km×rate + kg×2, escrow advance, nearest-online-driver dispatch, 10-min request expiry → reroute, PAUSED + retry |
| O5 | Pre-loading quality verification (helper photos, buyer approves/rejects before farmer is paid) | ✅ Done — 2–4 geotagged photos, buyer approve/reject, 15-min auto-approve, Start Journey locked until approved, 20% crop advance released at Start Journey |
| O6 | 2-way final delivery confirmation (transporter + buyer, auto-settle, farmer does nothing) | ✅ Done — DELIVERY_COMPLETED + GOODS_RECEIVED → COMPLETED → automatic settlement + notifications |

---

## 3. Scope

**In scope (proposal Table 1):** 4 roles; bidirectional marketplace (listings + wanted ads, search/filter); manual quality verification (JavaFX); automated vehicle selector + logistics calculator; order workflow engine; 2-way digital handshake; trust-based rating; live tracking.

**Out of scope (Table 2):** AI quality grading; real payment gateway (**mock wallet/escrow only** — never integrate a real gateway).

**Scope decisions made with the user (do not re-ask):**
- Roles in code: `FARMER`, `BUYER`, `TRANSPORTER`, `OFFICER` (legacy `ADMIN` account signs in with officer rights; **no separate admin role** — officers do monitoring).
- Transport "helper" uses the **driver's app** (no helper account).
- No cold-chain sensors / temperature / AI / QR gate pass / arbitrator / bank accounts (Figma shows them; decided out of scope 15 Nov).
- Maps: **OpenStreetMap (osmdroid) in the Android app** (Google Maps SDK needs prepaid billing the team declined); "Check in Google Maps" + paste coordinates is the accuracy aid; turn-by-turn opens the Google Maps app. The **backend** uses Google Distance Matrix (server key) with a straight-line fallback.
- Notifications are **in-app** (stored in `notifications`, polled); no FCM push, no SMS. Password reset codes go **by email** (6 digits).

---

## 4. Requirements and their status

Legend: ✅ implemented + verified (tests/live) · 🟡 implemented, partly verified or with limits · ❌ missing.

### 4.1 Functional requirements

| ID | Requirement (source) | Status | Notes / evidence |
|---|---|---|---|
| FR-AUTH-1 | Register + log in for each role, JWT (§4.1, §6.4.2) | ✅ | Farmer/buyer/transporter self-register (inline validation, availability check, map pin); officers seeded. Lockout after 5 failed logins (15 min). Live login verified 17 Nov for all three app roles |
| FR-AUTH-2 | Role-based access + ownership | ✅ | `SecurityConfig` role areas; `security/CurrentUser` checks; acting user always from the JWT |
| FR-AUTH-3 | Forgot / reset / change password | ✅ | Email code → verify → reset (all roles, app + desktop); in-app change password |
| FR-LIST-1 | Listing with real-time location-tagged photos (O1, workflow 1.2–1.4) | ✅ | `GET /listings/location-check` (500 m), camera only (gallery disabled), 3 media required, GPS metadata, estimated yield + price/kg |
| FR-LIST-2 | 10 s video | ✅ | CameraX `camera-video` 1.3.1, no audio |
| FR-LIST-3 | Listing lifecycle: Draft → Pending → Under Review → Listed / Retake / Rejected; edit, deactivate, reactivate | ✅ | Material edits of an ACTIVE listing send it back to PENDING |
| FR-VER-1 | Shared officer queue, claim lock, approve+grade / retake / reject (§6.7) | ✅ | Atomic `findAndModify` claim (409 if taken), release, orphaned claims recoverable, decision log |
| FR-VER-2 | Media authenticity check (timestamp + GPS vs farm) (§3.3.1) | ✅ | `OfficerPortalService` checks distance to farm, capture time, device; shown on review page |
| FR-MKT-1 | Buyer search/filter by crop, grade, location, quantity, price (§6.6) | ✅ | `GET /buyer/listings?search&grade&category&district&maxPrice&minQty` |
| FR-MKT-2 | Wanted ads (demand) + farmer supply offers | ✅ | Post/edit/close/republish; offer counts; accept offer → order; auto-close on accept or deadline |
| FR-MKT-3 | Market insights for farmers (workflow 1.1) | ✅ | `market_prices` collection (1,350 seeded rows) + completed orders → trend, centres, insight text; also on supply-offer form (17 Nov) |
| FR-CHAT-1 | In-app negotiation chat (O3) | 🟡 | Works both ways, timestamps exact (whole seconds); **polling** (proposal mentions Socket.io — acceptable deviation, explain in viva) |
| FR-ORD-1 | Strict order state machine (§6.9) | ✅ | `OrderService` + `POST /orders/{id}/events`; no free-form status PUT |
| FR-LOG-1 | Auto vehicle selection by weight (§6.8) | ✅ | Smallest of 4 classes that fits (500/1,000/4,000/10,000 kg) |
| FR-LOG-2 | Dynamic fee from distance + weight (§6.8, workflow 3.1) | ✅ | `FeeCalculator`; distance `DistanceService` (Google, fallback ×1.3) |
| FR-DISP-1 | Dispatch to nearest available driver, reroute on decline, pause when none (workflow 3.3) | ✅ | Nearest online + free + capacity + not suspended; 10-min expiry; PAUSED retried every 2 min. ⚠️ drivers who never toggled online (`online == null`) count as online — open decision §11.3 |
| FR-PAY-1 | Advance into escrow: 20% crop + transport + 3% fee (workflow 3.2) | ✅ | Card (simulated) or wallet; top-up |
| FR-PAY-2 | Release crop advance to farmer at Start Journey (workflow 4.3) | ✅ | `LedgerService` |
| FR-PAY-3 | 80% balance at acceptance; settlement 3% / 5% / 0% (workflow 5.1–5.2) | ✅ | Integration-tested |
| FR-PRE-1 | Pre-loading evidence + buyer approve/reject (O5, workflow 4.2) | ✅ | 2–4 photos, 15-min auto-approve, Start Journey locked |
| FR-TRACK-1 | Live tracking for buyer (§4.1) | ✅ | Driver foreground service posts GPS every 30 s; `GET /orders/{id}/tracking` (remaining km, ETA, progress); buyer map (OSM). 🟡 farmer tracker shows steps, not a map |
| FR-HS-1 | 2-way handshake + auto settlement (O6, workflow 5.2) | ✅ | |
| FR-DSP-1 | Pre-loading rejection: buyer 100% refund incl. fee; farmer pays Empty Trip Fee; negative balance → suspended (workflow 5.3) | ✅ | |
| FR-DSP-2 | Post-delivery rejection: buyer full refund; transporter charged crop advance, paid nothing; farmer keeps advance; return journey (workflow 5.3) | ✅ | Driver taps "Goods Returned" (GPS recorded) |
| FR-DSP-3 | Issue tickets / appeals reviewed by officers | ✅ | `disputes` collection; desktop resolve/dismiss with note (money unchanged, by decision) |
| FR-RATE-1 | Ratings + trust score (§6.10) | ✅ | Buyer rates farmer + driver; driver rates farmer + buyer; farmer rates buyer. Trust = 100 for new users, `round(100 × (0.6×avg/5 + 0.4×completed/(completed+failed)))`; recomputed on startup and after events |
| FR-ADM-1 | Monitor users, orders, disputes (§6.3.1) | ✅ | Desktop: users (suspend/reactivate with reason), orders + detail, disputes |
| FR-NOTIF-1 | Notifications ("Vehicle Assigned – Start Harvesting", "Delivery Successful – Funds Transferred", …) | ✅ | In-app list + bell popup (popup used fake data until 17 Nov — fixed) |
| FR-43 (report) | 24 h reminder | ❌ | Mentioned in the final report; not implemented |

### 4.2 Non-functional requirements (derived from proposal §6 + report)

| NFR | Target | Status |
|---|---|---|
| Security | JWT, role-based access, no secrets in git, hashed passwords/reset codes, DTOs only, upload type/size checks | 🟡 All code-side fixes done (S1–S18 in `DEVELOPMENT_STATUS.md` §2). **Secrets that were committed earlier must still be rotated by the team** (Mongo password, JWT secret, mail password, EC2 key) — old values remain in git history |
| Usability | Role-specific mobile UI, consistent brand (`#1B4332` / `#4CAF50` / `#E8F5EA`, Plus Jakarta Sans), Figma-aligned | 🟡 Farmer, buyer, transporter and desktop aligned to Figma; 17 Nov passes not yet checked on a device |
| Performance | Thumbnails for media (`/uploads/thumb/{name}?size=160|320|640`), paging on desktop, video streamed | ✅ |
| Reliability | State machine, scheduled jobs (auto-approve, request expiry, PAUSED retry), idempotent settlement | ✅ integration-tested |
| Maintainability | Layered backend, shared UI kits, config in `MarketplaceRules` | ✅ |
| Testability | Unit + integration + system tests | ✅ backend 52/52, desktop 14/14, Android 28/28; live system test scripts in `docs/testing-evidence/scripts/` |
| Portability / deploy | Cloud DB (Atlas), API on EC2 behind nginx + HTTPS | ✅ (manual deploy; ❌ no CI) |
| UAT | Real users | ❌ not run (report says "Not yet run" honestly) |

---

## 5. Architecture

```
 Android app (Java 8, XML)          JavaFX desktop portal (Java 21)
 Farmer · Buyer · Transporter        Agricultural Officer
          │  HTTPS + JWT (Retrofit)          │  HTTPS + JWT (java.net.http + Jackson)
          └───────────────┬──────────────────┘
                          ▼
        nginx (TLS, harvesthubapp.tech, 50 MB body) on AWS EC2 13.228.18.33
                          ▼
        Spring Boot 3.2.1 REST API (Java 21 target) — systemd service "harvesthub", 127.0.0.1:8080
        controller → service → repository (Spring Data MongoDB) · Spring Security + JJWT 0.11.5
        @Scheduled jobs: loading auto-approve, job-request expiry, PAUSED re-dispatch
        Media on server disk: /uploads/{name} (+ /uploads/thumb cache)
                          ▼
        MongoDB Atlas (team cluster)          External: Google Distance Matrix (server key),
                                              Open-Meteo weather, Gmail SMTP (reset codes),
                                              OpenStreetMap tiles (app side)
```

- **Layers (proposal §6.2):** frontend (Android, JavaFX) → backend (REST) → database (MongoDB Atlas) → external services. Matches the proposal; deviation: OSM instead of Google Maps on the phone (billing), polling chat instead of Socket.io.
- **Auth model:** `POST /api/auth/login` returns a JWT (subject = user id, role claim). Every other endpoint needs `Authorization: Bearer`. Public: login, register, check-availability, forgot/verify/reset password, `/auth/*-otp`, `GET /uploads/*`. Officer area needs OFFICER/ADMIN. The filter reloads the user per request (suspended users get 403).
- **URL layout:** most controllers are mapped at both `/api/...` and `/...` (`@RequestMapping({"/api", ""})`); the Android base URL is `https://harvesthubapp.tech/` and its paths have no `/api` prefix (except auth).
- **Config:** env-driven, no secrets in `application.properties`. Server: `/etc/harvesthub/harvesthub.env` (systemd override) with `MONGODB_URI`, `MONGODB_DATABASE`, `JWT_SECRET` (≥32 bytes, app refuses to start without it), `MAIL_USERNAME`, `MAIL_PASSWORD`, `GOOGLE_MAPS_API_KEY`, optional `SEED_DEMO_DATA`, `DEMO_PASSWORD`. Local: untracked `harvesthub-backend/application-local.properties` (copy from `.example`).
- **Business constants:** `config/MarketplaceRules.java` (`harvesthub.rules.*`): buyer fee 3%, transporter commission 5%, advance crop rate 20%, handling Rs 2/kg, road factor 1.3, farm radius 500 m, loading approval 15 min, truck speed 40 km/h, job request 10 min, loading photos 2–4, vehicles (type, label, capacity kg, Rs/km): THREE_WHEELER 500/80, MINI_LORRY 1,000/120, LORRY 4,000/200, TRUCK 10,000/350.

---

## 6. Components

### 6.1 Backend — `harvesthub-backend/` (package `com.harvesthub.backend`)
- **Controllers (21):** `AuthController` (login/register/reset), `MeController` (`/api/me/*`: password, preferences, delivery location, vehicle, availability), `ListingController` (create, retake, edit/deactivate/reactivate, location-check), `FarmerController` (active listings incl. negotiation info), `BuyerListingController` (marketplace + filters), `BuyerController` (buyer dashboard), `SupplyOfferController` (offers + accept), `OrderController` (create, events, payment, confirm-deal, tracking, disputes, rate), `ChatController`, `LogisticsController` (legacy preview), `MarketController` (trend/centres/insight), `DashboardController` (farmer/transporter dashboards), `TransporterController`, `WalletController` (summary, transactions, deposit, withdraw), `NotificationController`, `MediaController` (upload, `/uploads/{name}`, `/uploads/thumb/{name}`), `OfficerController` + `OfficerPortalController` (`/api/officer/**`), `UserController` (admin), `BatchController`/`OtpController` (legacy).
- **Services (26):** `OrderService` (**the state machine**), `DispatchService` (nearest driver), `LedgerService` (all money moves; wallets PLATFORM + ESCROW), `FeeCalculator`, `DistanceService`, `TrackingService`, `DisputeService`, `TrustService`, `ListingService`, `OfficerPortalService`, `MediaStorageService` (magic-byte checks, generated names, thumbnails), `MarketService`, `WeatherService`, `NotificationService`, `EmailService`, dashboards (`DashboardService`, `BuyerDashboardService`, `TransporterDashboardService`), `ChatService`, `WalletService`, `UserService`, etc.
- **Collections (MongoDB):** `users` (role, contact, farmLocation = farm / delivery point / vehicle base, trustScore, vehicle fields, online, suspended), `listings` (media URLs + GPS, status, grade, reviews[]), `wanted_ads`, `supply_offers`, `orders` (status, statusHistory[], transportEvents[], payment{}, confirmations, loading evidence, ratedBy[]), `logistics`, `chat_messages`, `wallets`, `transactions`, `wallet_summaries`, `ratings`, `disputes`, `notifications`, `market_prices`, `crops`, `crop_batches` (legacy, unused).
- **Seeders:** `config/DataInitializer` (opt-in demo accounts via `SEED_DEMO_DATA`), `config/TestDataSeeder` (profile `seedtest`; phases for farmer/buyer/transporter/officer views; inserts only, tagged `@test.harvesthub.lk`).
- **Tests (`src/test`):** unit (`FeeCalculatorTests` incl. trust, `JwtUtilTests`, `PasswordReset*`, `SecurityRulesTests`) + integration on **local** Mongo `harvesthub_it` (`OrderLifecycle`, `BuyerFlow`, `TransporterFlow`, `OfficerPortal`). **52/52 pass** (17 Nov). Tests never touch Atlas.

### 6.2 Android — `harvesthub-android/` (package `com.harvesthub.mobile`, Java 8 source, minSdk 21, compileSdk 34)
- `network/ApiService.java` (all Retrofit endpoints), `network/ApiClient.java` (one authenticated client, set up in `HarvestHubApp`), `utils/SessionManager` (token, user, `getUserId()`), `utils/Constants.BASE_URL = BuildConfig.API_BASE_URL`.
- `ui/common/`: `LauncherActivity` (routes by role), `RoleSelectionActivity` (landing), `PinLocationActivity` (one OSM pin screen for farm / delivery point / vehicle base), `RoleHome` (after registration → sign in → role home), `MediaViewerActivity`, `ZoomableImageView`.
- `ui/farmer/` (44 files): `MainActivity` hosts tabs Home / Listings / Market / Wallet / Settings; registration, login, forgot-password chain, `CameraCaptureActivity` + `CreateListingFragment`, `ListingsFragment` + `ListingAdapter` (status cards, edit/deactivate/reactivate, negotiation badge), `ChatActivity`, order-step screens built on `BaseOrderActivity` (wait for payment, start harvesting, harvest ready, vehicle arrival, in transit, rating), `CreateSupplyOfferActivity`, `NotificationsActivity` (shared by all roles).
- `ui/buyer/` (44 files): `BuyerDashboardActivity` (bottom bar Home / Market / Orders / Requests / Account; Offers opens from Requests and Home), marketplace + `FilterMarketplaceBottomSheet` + purchase request, wanted ads, supply offers, `NegotiationChatActivity`, order screens on `BaseBuyerOrderActivity` routed by `BuyerOrderRouter` (OrderSummary → SecureCheckout → TransactionLedger → HarvestTracker → VerifyCargo → TransitTracking → ArrivalInspection → DeliveryHandshake → BuyerRating / DisputeResolved), wallet, profile.
- `ui/transporter/` (27 files): screens are **built in code** from the view kit `TUi` + `res/layout/transporter_scaffold.xml` (header, scroll content, action area, Home/Jobs/History/Settings bar). `BaseTransporterOrderActivity` + `TransporterOrderRouter` route by order status: JobOffer → ArrivingPickup → LiabilityCheckpoint (evidence camera) → LoadingApproval → NavigationTracking → DestinationArrived → RateExperience; CargoRejected / DeliveryDisputed / DisputeCase / AppealDispute; `TransporterTrackingService` (foreground GPS every 30 s); `RouteMap` (OSM).
- Status → screen routers are unit-tested (`OrderFlowRouterTest`, `BuyerFlowTest`, `TransporterFlowTest`). **28/28 unit tests pass.**
- The old `transporter_activity_*.xml` layouts and ~10 legacy activities (`ListingsHistoryActivity`, `MarketInsightsActivity`, `FarmerRegisterActivity`, buyer `VerificationCode*`, buyer `CreateNewPasswordActivity`, `VerifiedHarvestsActivity`, `SupplyOffersActivity`, `MyDemandRequestsActivity`, `MarketplaceSearchActivity`, `RouteMapActivity`, `BuyerOrdersActivity`) are **unreachable leftovers** (some contain fake data). Safe to delete after confirming no reference; user hasn't decided yet.

### 6.3 Desktop — `harvesthub-desktop/` (JavaFX 21.0.6, module `com.harvesthub.desktop`)
- `PortalApp` (entry), `api/ApiClient` + `Session`, `ui/Shell` (dark sidebar), `ui/Ui` (component kit: cards, crop tiles with thumbnail cache, segmented tabs, stat cards), `ZoomImageView`, pages: Login (officer-only, email-code reset), Dashboard, Queue, Review (photos with zoom, inline video via temp file, GPS/time checks, Google Maps links, grade A/B/C, retake/reject, release), History, Users (+detail, suspend/reactivate), Orders (+detail), Disputes (+detail, resolve/dismiss), Profile, Notifications. Styles in `portal.css`; Plus Jakarta Sans bundled. **Tests 14/14.** Packaged app: `harvesthub-desktop/dist/HarvestHub/HarvestHub.exe`.

---

## 7. End-to-end workflow, state machine and data flow

### 7.1 Listing (Phase 1)
| Step | Actor / screen | API | Listing status |
|---|---|---|---|
| 1.1 Market insight | Farmer → Market tab | `GET /market/trend`, `/market/centers` | — |
| 1.2 Location lock | Farmer → New listing | `GET /listings/location-check` (≤500 m from registered farm) | DRAFT |
| 1.3 Guided capture | `CameraCaptureActivity` (wide photo, close-up, 10 s video; gallery disabled; GPS embedded) | `POST /media/upload` ×3 | DRAFT |
| 1.4 Submit | Listing Details form (estimated yield kg, price/kg) | `POST /listings/create` | PENDING |
| 1.5 Officer check | Desktop Queue → claim → Review | `/api/officer/queue`, claim, approve(grade)/retake/reject | UNDER_REVIEW → ACTIVE ("Listed") / RETAKE_REQUESTED / REJECTED |
Farmer card actions: edit (yield/price; material edit → PENDING), deactivate → DEACTIVATED, reactivate → previous status. When a deal is accepted the listing becomes ONGOING; it shows "IN NEGOTIATION" while a buyer negotiates.

### 7.2 Order state machine (`OrderService`, events via `POST /orders/{id}/events`)
```
NEGOTIATING ──(both confirm same quantity/price/date; any change = counter-offer)──► AWAITING_PAYMENT
AWAITING_PAYMENT ──buyer pays advance (card|wallet) POST /orders/{id}/payment──► DISPATCHING
DISPATCHING ──nearest driver found──► TRANSPORTER_ASSIGNED ──ACCEPT──► TRANSPORTER_ACCEPTED
            └─none──► PAUSED (retry every 2 min)   └─DECLINE / 10-min expiry → next driver
TRANSPORTER_ACCEPTED ──farmer START_HARVESTING──► HARVESTING ──HARVEST_READY──► HARVEST_READY
HARVEST_READY ──driver ARRIVED_PICKUP──► ARRIVED_AT_PICKUP ──LOADING_EVIDENCE_SUBMITTED (2–4 photos)──► AWAITING_BUYER_LOADING_APPROVAL
   ├─buyer APPROVE_LOADING (or 15-min auto-approve)──► LOADING_APPROVED ──driver START_JOURNEY (20% crop advance → farmer; GPS on)──► IN_TRANSIT
   └─buyer REJECT_LOADING──► PRELOAD_REJECTED (buyer 100% refund incl. fee; farmer pays Empty Trip Fee → driver)
IN_TRANSIT ──LOCATION pings (30 s)… driver ARRIVED_DESTINATION──► ARRIVED_AT_DESTINATION
   ├─buyer ACCEPT_DELIVERY (pays 80% balance)──► BALANCE_PAID ──DELIVERY_COMPLETED + GOODS_RECEIVED (either order; DELIVERED = one side done)──► COMPLETED (settlement)
   └─buyer REJECT_DELIVERY──► RETURN_JOURNEY (buyer full refund; driver charged crop advance, unpaid) ──driver GOODS_RETURNED──► RETURNED
COMPLETED → ratings (POST /orders/{id}/rate with toRole) → trust scores recalculated
```
Who sends what: farmer — confirm-deal/counter, START_HARVESTING, HARVEST_READY; buyer — create order / accept offer, confirm-deal, payment, APPROVE/REJECT_LOADING, ACCEPT/REJECT_DELIVERY, GOODS_RECEIVED, disputes; transporter — ACCEPT/DECLINE, ARRIVED_PICKUP, LOADING_EVIDENCE_SUBMITTED, START_JOURNEY, LOCATION, ARRIVED_DESTINATION, DELIVERY_COMPLETED, GOODS_RETURNED; system — dispatch, expiry, auto-approve, settlement, notifications.

### 7.3 Which screen shows each status
| Status | Farmer | Buyer | Transporter |
|---|---|---|---|
| NEGOTIATING | ChatActivity (listing card "IN NEGOTIATION") | NegotiationChat | — |
| AWAITING_PAYMENT | WaitingForAdvancePayment | OrderSummary → SecureCheckout | — |
| DISPATCHING / PAUSED / TRANSPORTER_ASSIGNED | waiting screen | TransactionLedger | JobOffer (assigned driver only) |
| TRANSPORTER_ACCEPTED / HARVESTING / HARVEST_READY | StartHarvesting / HarvestReady | HarvestTracker | ArrivingPickup |
| ARRIVED_AT_PICKUP | VehicleArrival | HarvestTracker | LiabilityCheckpoint (Arrived at Farm) |
| AWAITING_BUYER_LOADING_APPROVAL / LOADING_APPROVED | WaitForBuyerApproval | VerifyCargo | LoadingApproval (Buyer Approval / Ready for Departure) |
| IN_TRANSIT | OrderInTransit | TransitTracking (map) | NavigationTracking (map) |
| ARRIVED_AT_DESTINATION / BALANCE_PAID / DELIVERED | ActiveOrderTracker | ArrivalInspection → DeliveryHandshake | DestinationArrived (arrival / handshake) |
| COMPLETED | RatingFeedback | BuyerRating | RateExperience |
| PRELOAD_REJECTED | tracker + wallet charge | DisputeResolved | CargoRejected |
| RETURN_JOURNEY / RETURNED | tracker | DisputeResolved | DeliveryDisputed → DisputeCase / AppealDispute |
Routers: farmer `OrderFlowRouter`, buyer `BuyerOrderRouter`, transporter `TransporterOrderRouter`. Each order screen polls the order every 10 s and hands over to the next screen when the status changes.

### 7.4 Data flow (example: one deal)
Buyer app `POST /orders/create` → `orders` doc (NEGOTIATING) + `chat_messages` → both confirm → `OrderService` computes vehicle + distance + fee (`logistics` doc) and the payment quote (`orders.payment`) → buyer pays → `LedgerService` debits buyer (wallet) or records card deposit, credits ESCROW (`transactions`, `wallets`) → `DispatchService` picks driver, writes `transporterId` + expiry, `notifications` → driver events append `transportEvents[]` (with GPS) and `statusHistory[]` → evidence photos in `/uploads`, URLs on the order → Start Journey: ESCROW → farmer wallet (20% crop) → GPS `LOCATION` events feed `TrackingService` → buyer accepts: buyer → ESCROW (80%) → handshake → ESCROW → farmer (80%), driver (95% transport), PLATFORM (3% + 5%) → `ratings` → `TrustService` updates `users.trustScore`. Officer portal reads the same collections through `/api/officer/**`.

---

## 8. Money, logistics and trust rules (decided; implemented in `LedgerService`, `FeeCalculator`, `TrustService`)
- **Transport fee** = distance_km × vehicle rate/km + weight_kg × 2. Distance = Google road distance, else straight line × 1.3.
- **Advance** (buyer pays first) = 20% of crop value + transport fee + 3% × (crop + transport). Goes to ESCROW.
- **Start Journey:** 20% crop portion → farmer.
- **Acceptance at delivery:** buyer pays the 80% crop balance into ESCROW.
- **Completion:** farmer gets the 80% (0% fee), driver gets 95% of the transport fee, platform keeps 3% buyer fee + 5% commission.
- **Pre-loading rejection:** buyer refunded 100% incl. 3% fee; farmer charged the Empty Trip Fee (= full transport fee) which goes to the driver; negative farmer balance → suspended (cannot list).
- **Post-delivery rejection:** buyer refunded 100%; driver earns nothing and is charged the crop advance; farmer keeps the advance; negative driver balance → suspended (no jobs) until mock top-up "Pay back".
- **Appeals/issue tickets** never move money (officer adds a note).
- **Trust score** = 100 for new users; `round(100 × (0.60 × avgRating/5 + 0.40 × completed/(completed+failed)))`.
- Payments: **mock only** — simulated card form or mock wallet (top-up, withdraw).

---

## 9. UI inventory (Figma vs built)

Figma file key `fmEOnxONufOFuBF63H0deN` (pages: Farmer view `70:905`, Buyer view `413:4`, Transporter view `612:58`); transporter PNG exports in `docs/figma/transporter/`. The Figma MCP free plan hit its call limit — the user now pastes frame images in chat.

| Area | State |
|---|---|
| Landing / role selection, farmer login + registration + forgot password, Pin location (shared) | Rebuilt from Figma (14–16 Nov) |
| Farmer: home, listings (all status cards), camera, listing form, market, wallet, settings, chat, order steps | Wired to real data; camera + listing cards + details rebuilt to the user's designs (16 Nov); other farmer screens follow Figma content but were not pixel-matched |
| Buyer shell (17 Nov later): 5-tab bar Home · Market · Orders · Requests · Account; Home, Orders, Account and Harvest Tracker rebuilt with the shared `TUi` kit | Done, installed, not yet reviewed by the user |
| Buyer: all 15 screens the user sent on 17 Nov (Order Summary, Secure Checkout, Ledger, Verify Cargo, tracking, Arrival Inspection, Handshake, Rating, Dispute Resolved, Verified Harvests, Filter, Post Demand, My Demand Requests, Supply Offers, Negotiation chat) | Match; rating screen rebuilt, tracking map moved to OSM, offers banner added (17 Nov) |
| Transporter: all 20 screens sent on 17 Nov | Recomposed in code to the frames (17 Nov); data/events unchanged |
| Desktop officer portal | No Figma; designed to inspiration images (17 Nov refresh) |

**Screens designed without a Figma frame (styled to match; need a frame only if exactness matters):** Buyer Account tab, Transporter Wallet, Transporter Account Suspended, buyer loading-evidence photo viewer, full-screen media viewer (farmer + buyer), buyer "Report issue" dialog, buyer Home dashboard / Wallet / Profile / Harvest tracker (followed Figma text only — frames had been read as text when the Figma limit hit). **No screen required by the workflow is missing.**

Deliberate design deviations (all user-approved): Order Summary says "Crop Advance (20%)" (Figma's "Partial Crop Cost" numbers were inconsistent); checkout has a card/wallet choice; email field kept on buyer sign-up (reset by email); 6-digit codes (Figma says 4); no cold-chain/sensor/bank widgets; transporter Jobs "Shortest Trip" instead of "Nearest Pickup"; no farmer/buyer call buttons on transporter screens (no phone numbers exposed).

---

## 10. Current progress (as of 2026-11-17 evening)

| Area | Progress | Verified by |
|---|---|---|
| Backend | Feature-complete for the proposal; deployed (latest JAR sha256 `285df750…614d`, 17 Nov evening) | 52/52 tests; live HTTPS checks; live end-to-end order (17 Nov) |
| Android | All three roles feature-complete; Figma passes for buyer + transporter done 17 Nov; data audit done | Build + 28/28 unit tests; live login + dashboard/listings/notifications/wallet 200 for all roles. **Latest APK not yet installed / clicked through on a phone** |
| Desktop | Feature-complete; visual refresh deployed as packaged exe | 14/14 tests; off-screen snapshot checks |
| Data | Atlas has tagged test data for every stage (`@test.harvesthub.lk`); backups in `backups/` (git-ignored) | Seeder dry runs + read-only checks |
| Docs | Audits per role, testing report, report chapters, this handover | — |
| Report | v2 with supervisor fixes outside the repo (`…\Final Presentation\Report\Changes\ICT_2212_Group12_Codex_Final_Report_v2.docx`) | Open items in §11.4 |

Overall: **functionally complete (≈95%)**; remaining work is verification on a real phone, small polish, git hygiene, and presentation prep.

---

## 11. Gaps, risks and open items (priority order)

### 11.1 Critical before the presentation
1. **Commit and push the work.** The user committed everything up to `c0de213`; the 17 Nov evening fixes (§16 last row) are uncommitted. One disk failure loses it. Commit only with the user's OK; never commit `application-local.properties`, `local.properties`, `*.pem`, `backups/`, `.env`. Then (with OK) open a PR into `main`.
2. **Install and click through the latest APK** on the demo phone for all three roles (buyer + transporter UI passes of 17 Nov were compile-checked only; transporter screens are built in code, so small spacing/wrapping issues are likely). Build with the production URL (see §12.2).
3. **Demo data readiness:** pick demo accounts (§12.4) and walk one order end-to-end the evening before; the test order `6afb98943723db762ba52287` was left open on test accounts.

### 11.2 Known functional limits (acceptable; explain if asked)
- Chat is polling (proposal mentioned Socket.io).
- Notifications are in-app only (no push/SMS); password reset codes by email only.
- Maps draw the route along real roads with the public OSRM demo router (`utils/RoadRoute`, cached, no login token sent); if it is unreachable the map falls back to a straight line. Turn-by-turn opens Google Maps. The OSRM demo server is fair-use only — fine for the demo, swap for a hosted router before a real launch.
- Live position = driver phone GPS every 30 s (`TransporterTrackingService`) — needs location permission + the trip notification on the driver's phone.
- FR-43 24 h reminder not implemented.
- Legacy endpoints remain: `/auth/*-otp`, `LogisticsController` + `LogisticsService.estimateDistance` (fixed city table, not used by the app), `crop_batches`.
- ~10 unreachable legacy Android screens + old `transporter_activity_*.xml` layouts (§6.2).
- `lintDebug` last run 13 Nov.
- No CI/CD; deploy is manual.

### 11.3 Decisions still open (ask the user)
- Should drivers who never switched online/offline (`online == null`) count as online for dispatch? Now: yes. Strict alternative: `DispatchService.candidates` filter `Boolean.TRUE.equals(t.getOnline())` (+ default in `UserDto`/`TransporterDashboardService`), then test + deploy with approval.
- Delete the unreachable legacy screens? (pending answer)

### 11.4 Team-owned items (not code)
- **Rotate leaked secrets** (Mongo Atlas password, JWT secret, Gmail app password, EC2 key) — then update `/etc/harvesthub/harvesthub.env` and local `application-local.properties`.
- Report: use case diagram (add UC023, fix duplicate officer use case, include/extend arrows), more officer-portal screenshots, member contribution rows, refresh fields (F9), re-export PDF; UAT not run (do not invent results).
- Before release: clean and re-seed Atlas (only with explicit go-ahead + backup).

---

## 12. How to continue

### 12.1 Environment (user's Windows laptop)
- JDK 25/26 installed (no JDK 21; builds still target 21 and work). Maven wrapper in backend/desktop; Gradle wrapper in Android.
- adb at `D:\Android\platform-tools\adb.exe`; phone is a Xiaomi (MIUI: "Install via USB" + "USB debugging (Security settings)" must be on).
- Working mode (user decision 14 Nov): **everything runs against the remote stack** (harvesthubapp.tech + Atlas). No local backend for the apps.
- Machine has low free memory — use the low-memory flags below.

### 12.2 Commands
```powershell
# Backend tests (from harvesthub-backend; local Mongo service on :27017 for integration tests)
$env:MAVEN_OPTS="-Xmx256m -Xss512k -XX:TieredStopAtLevel=1 -XX:+UseSerialGC"
.\mvnw.cmd -o test "-DargLine=-Dnet.bytebuddy.experimental=true -Xmx512m -Xss512k -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m"
# results: target\surefire-reports\*.xml

# Android build + unit tests (from harvesthub-android) — ALWAYS pass the API URL
.\gradlew.bat assembleDebug testDebugUnitTest --offline "-PapiUrl=https://harvesthubapp.tech/"
# check: app\build\generated\source\buildConfig\debug\com\harvesthub\mobile\BuildConfig.java → API_BASE_URL
D:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

# Desktop (from harvesthub-desktop)
.\mvnw.cmd -q clean test javafx:jlink
# then jpackage → target\pkgN, copy into dist\HarvestHub (close the running portal first; Windows locks it)

# User's own run scripts (repo root)
powershell -File scripts\run-mobile.ps1 -Production
powershell -File scripts\run-desktop.ps1 -Production
```
⚠️ **Login failure cause seen on 17 Nov:** untracked `harvesthub-android/local.properties` contains `API_URL=http://172.31.99.217:8080/` (old laptop IP). Any Android build without `-PapiUrl=https://harvesthubapp.tech/` points the app there and every login shows "check your internet connection". Either always pass `-PapiUrl` or delete that line.

### 12.3 Deploy the backend (only when the user explicitly says "deploy")
EC2 `ec2-user@13.228.18.33`, key `harvesthub-backend/harvesthub_key_pair.pem` (never commit/print), service `harvesthub`, jar `/home/ec2-user/app.jar`.
1. `cd harvesthub-backend && ./mvnw.cmd -o -q package -DskipTests`
2. On server: `cp -p ~/app.jar ~/app.jar.bak-<yyyymmdd>-<tag>`
3. Upload in 4 MB parts (`split -b 4m`, scp parts, `cat` on server) — large scp transfers get reset; compare `sha256sum` both sides.
4. `mv` into place → `sudo systemctl restart harvesthub` → wait for "Started HarvesthubBackendApplication" in `journalctl -u harvesthub`.
5. HTTPS smoke checks; add a row to `DEVELOPMENT_STATUS.md` §6 with sha256, backup name and rollback command (`cp ~/app.jar.bak-… ~/app.jar && sudo systemctl restart harvesthub`).
Never run `deploy*.ps1` unless the user asks.

### 12.4 Test accounts (password = `DEMO_PASSWORD` in `application-local.properties`; never print it)
- Farmers: `nimal.farmer@`, `kamala.farmer@`, `sampath.farmer@test.harvesthub.lk`
- Buyers: `roshan.buyer@`, `keells.buyer@`, `greenmart.buyer@test.harvesthub.lk`
- Drivers (log in by mobile): tharindu `0700000209` (pending request + history), others seeded at every stage (pradeep, lahiru, mahesh, sunil, dilan, chamara, isuru = suspended, samuel); Kasun Fernando is the nearest Dambulla driver.
- Officer: `officer.nadeesha@test.harvesthub.lk`
- If a login returns 401, stop after 1–2 tries (5 failures lock the account for 15 min) and ask the user — testers sometimes change passwords.
- Live smoke script idea: log in via `POST /api/auth/login {emailOrMobile, password}`, then GET `dashboard/{role}/{id}`, `notifications/{id}`, `wallet/{id}/summary` (no `/api` prefix needed).

### 12.5 Database safety
Atlas is shared live data. Inserts of tagged test data are allowed; **no deletes/drops/updates of others' data without the user's explicit go-ahead and a backup first** (`backups/`, git-ignored). Integration tests use local Mongo only.

---

## 13. Engineering approach used (follow the same)
1. **Scope authority:** proposal > workflow PDF > existing code. Don't invent features; record conflicts and ask.
2. **Audit before building:** for each role, compare Figma vs proposal/workflow vs app vs backend vs real data; write `docs/<ROLE>_AUDIT.md`; ask the user about every conflict in one batch; record decisions.
3. **Backend first:** endpoint + rule in a service + unit/integration test → client wiring → run → update `DEVELOPMENT_STATUS.md`. State changes only through `OrderService` events; money only through `LedgerService`.
4. **Security by default:** every non-auth endpoint needs JWT + role; acting user from the token (`CurrentUser`), never from path/body; return DTOs, never entities; uploads only via `MediaStorageService`.
5. **Real data only:** no hardcoded fallback rows/names/amounts in UIs; seed tagged test data instead.
6. **UI:** brand `#1B4332` / `#4CAF50` / `#E8F5EA`, Plus Jakarta Sans (use real font faces, not system bold), match the user's Figma frames; ask before redesigning; shared kits (`TUi`, desktop `Ui`, `PinLocationActivity`, routers) instead of copy-paste.
7. **Verification honesty:** say what was tested and what wasn't (e.g. "compiled, not checked on a phone"); never fabricate test or UAT results.
8. **User preferences:** the user tests the Android UI by hand — build + install only, no adb UI testing unless asked; don't open windows on their screen without asking; deploy/commit/push only when told; write in plain language.

---

## 14. Decision log (condensed; full lists in the audit docs)
- 13 Nov: security baseline; work on `fix/day1-security`.
- 14 Nov: remote-only working mode; Atlas test data allowed (tagged); farmer workflow + money rules (20% advance, 3%/5%/0%, Empty Trip Fee = full transport fee); 2-way handshake; email kept on sign-up; 6-digit codes.
- 15 Nov buyer: payment by card or wallet; real GPS tracking; 15-min auto-approve; "Report issue" = dispute ticket (no auto refund after acceptance); delivery pin required; wanted ads auto-close.
- 15 Nov transporter: sign-up with vehicle + base + licence photo; 10-min job requests; background GPS; geo checks never block; suspended = negative wallet; helper = driver's app; online switch; Jobs = only requests sent to this driver; no cold-chain; 4 vehicle classes; 2–4 loading photos; driver rates farmer + buyer.
- 15 Nov desktop: officers only (no admin role); resolve disputes with a note; suspend/reactivate; name "HarvestHub Officer Portal".
- 16 Nov: OpenStreetMap in the app (no Google Maps billing); one shared pin screen; all roles land on their home after registration; Android tested by the user by hand.
- 17 Nov: listing card rules ("Recommended rules": edit/deactivate/reactivate), trust starts at 100%, chat timestamps to the second, buyer + transporter Figma passes, notification popup uses real data.

---

## 15. Recommended next steps (in order)
1. Get the user's OK → commit everything on `fix/day1-security` (check `git status` for secrets first) → push → PR to `main`.
2. Build with `-PapiUrl=https://harvesthubapp.tech/`, install, and click through: farmer listing → officer approval (desktop) → buyer purchase/chat/pay → driver accept → farmer harvest → driver photos → buyer approve → transit → arrival → handshake → ratings. Fix whatever looks broken (screenshots from the user).
3. Prepare the demo script with seeded accounts at each stage (so the presentation doesn't depend on a full live run).
4. Optional polish if time: farmer live map, delete legacy screens, `lintDebug`, online-null dispatch decision.
5. After the presentation: rotate secrets, clean + re-seed Atlas (with backup), add CI (GitHub Actions: backend tests + Android assemble), merge to `main`.

---

## 16. Change log of this file
| Date | By | Change |
|---|---|---|
| 2026-11-17 | Claude (Opus 5.5) | Public landing page + downloads deployed at https://harvesthubapp.tech/ (source in `website/`; nginx `/site/` and `/downloads/` static routes; to update: re-copy `website/` files to `/usr/share/nginx/harvesthub/`). The APK offered is the debug build pointed at production (release build is unsigned). |
| 2026-11-17 | Claude (Opus 5.5) | Live tracking fixes: new farmer "Live Delivery" screen (`OrderInTransitActivity`, real OSM map + ETA/route/driver/payment/steps) and real map in the farmer tracker (fake `DeliveryMapView` deleted); road-following routes (`RoadRoute` + shared `TripMap`, also buyer tracking and transporter `RouteMap`); pickup date on transporter job offer / pickup screen / job cards (backend adds `pickupDate` to the job DTO — deployed 17 Nov, JAR `285df750…614d`); fixed buyer crash loop after paying the 80% balance (`DeliveryHandshakeActivity` cast `AppCompatButton` to `MaterialButton`); checkout no longer opens a screen behind the success dialog. Build + 28/28; backend compiles; install was blocked on the phone (accept the prompt). Rollback point = commit `c0de213`. |
| 2026-11-17 | Claude (Opus 5.5) | Buyer UX pass: 5-tab bottom bar, new Account tab, Home/Orders/Harvest Tracker rebuilt with TUi kit, wallet rows; installed |
| 2026-11-17 | Claude (Fable 5.2 / Opus 5.5) | Created: full project analysis vs proposal + workflow, architecture, workflow/state machine, rules, UI inventory, progress, gaps, continuation guide |
