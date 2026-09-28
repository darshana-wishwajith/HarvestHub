# HarvestHub - Farmer View & System Progress (75% Milestone Tracker)

> **Generated / Updated**: September 2026  
> **Target Goal**: Complete HarvestHub project up to 75% (Frontend Android, Backend Spring Boot REST APIs, MongoDB Database, Full End-to-End Functionality) with exact visual alignment to the design mockups in `Farmer view of Mobile App.zip`.

---

## 1. Overall Status Summary

| Layer | Status | Completion % | Notes |
|---|---|---|---|
| **Android Farmer View UI** | **Aligned & Built** | **85%** | All 18 screens visually and structurally aligned with design mockups |
| **Android API Integration** | **Integrated** | **80%** | SessionManager, Retrofit ApiService, and real endpoints connected |
| **Backend API Services** | **Implemented** | **85%** | Auth, Listings, Orders, Dashboard, Market Centers, Wallet APIs |
| **Database & Models** | **Synchronized** | **85%** | MongoDB entities and Android models matched |
| **Build & Compilation** | **VERIFIED SUCCESS** | **100%** | `assembleDebug` & Java compile passing with 0 errors |

---

## 2. Design Mockup vs Built Screen Audit & Alignment

Every screen has been cross-referenced against the mockups extracted from `Farmer view of Mobile App.zip`:

### 1. Authentication & Onboarding
* **1.1 Login Portal (`activity_farmer_login.xml`)**:
  - Aligned header margins (removed excessive 100dp top gap).
  - Fixed brand wordmark typography (`Harvest` in dark green, `Hub` in accent green).
  - Converted Sign In button to forest green (`#1D3B2D`) with 12dp rounded corners and elevation.
  - Corrected label punctuation and registration footer link (`Register Farm`).
* **1.2 Profile Registration Form (`activity_register.xml` & `RegisterActivity.java`)**:
  - Aligned with mockup: Hidden unnecessary email input field to match the exact 5-field registration design (`Full Name`, `Mobile Number`, `NIC`, `Create Password`, `Confirm Password`).
  - Added NIC info tooltip icon `(i)`.
  - Auto-derives username/email for Spring Security backend compatibility (`mobile@harvesthub.lk`).
  - Switched from client-side SHA-256 pre-hashing to raw password transmission so Spring Security BCrypt hashes correctly.
* **1.3 Farm Location Setup (`activity_location_setup.xml`)**:
  - Google Map integration with center GPS lock pin, pulse radius, search field, and `Confirm Coordinates` button.

### 2. Dashboard & Hub Navigation
* **2.1 Home & Market Insights Dashboard (`fragment_home.xml` & `HomeFragment.java`)**:
  - Aligned greeting to `Welcome back, Kamal!` (or dynamic session name).
  - Mint green Total Earnings card (`#EAF5EE`, `LKR 125,000`).
  - 2-card horizontal stats row: `ACTIVE LISTINGS` (orange badge) and `COMPLETED` (green checkmark).
  - Market Trends Card: cubic bezier LineChart, menu button (`...`), and green demand alert pill (`High demand for Big Onions in Dambulla`).
  - Full-width `(+) Create New Listing` button in forest green (`#1D3B2D`).
* **2.2 My Listings / Inventory Hub (`fragment_listings.xml` & `ListingsFragment.java`)**:
  - Revenue & Trust Score summary cards (`LKR 42.5k` / `94%`).
  - 3-tab layout: `Active`, `History`, `Wanted Ads`.
  - Search bar and status badge chips (`PENDING`, `DRAFTING`, `ACTIVE`).
* **2.3 Wallet Screen (`fragment_wallet.xml` & `WalletFragment.java`)**:
  - Balance card with `Withdraw Funds` action button.
  - Trust score indicator (`4.9 / 5.0 ★`).
  - Empty Trip Penalties notice banner (`LKR 0`).
  - Recent transactions list (`Order #402 Settled`, `Order #398 Settled`).
* **2.3.1 Settings Screen (`fragment_settings.xml` & `SettingsFragment.java`)**:
  - Profile avatar with initials, phone, and NIC tag (`NIC: 852145659V`).
  - Option rows: `Change Password`, `Notification Preferences`, `App Language`, `Help & Support`, and red `Log Out` button.

### 3. Listing & Validation Workflow
* **3.1 Location-Lock Validation (`activity_location_verification.xml`)**:
  - Circular GPS scanning radar with central marker and progress indicator.
* **3.2 Camera Capture (`activity_camera_capture.xml`)**:
  - Custom camera viewfinder overlay with bracket guides and shutter control.
* **3.3 Listing Submission Form (`fragment_create_listing.xml` & `CreateListingFragment.java`)**:
  - Photo thumbnail horizontal reel with `(+)` add square.
  - Crop dropdown, estimated yield input with weight icon, expected price input with rupee icon.
  - Pricing insight tip card (`Current average market rate is LKR 250/kg`).
  - Full-width `Submit Listing` button with checkmark icon.

### 4. Buyer Demands & Supply Offers
* **4.1 Buyer Wanted Ads Feed (`item_wanted_ad.xml` & `MarketFragment.java`)**:
  - Demand cards with `URGENT` badge, required volume, target price, and `Send Offer` button.
* **4.2 Supply Offer Creation (`activity_create_supply_offer.xml`)**:
  - Responding to demand banner, verified batch selector dropdown, quantity and price fields, and `Send Offer to Buyer` button.

### 5. Order Fulfillment & Logistics Tracking
* **5.1 Active Order Tracker (`activity_active_order_tracker.xml`)**:
  - Funds in escrow status header (`Order #ORD-892`).
  - 4-step vertical progress timeline:
    1. `Buyer Payment Cleared` (✓ Done)
    2. `Transporter Assigned & En Route` (⦿ Current active step with ETA)
    3. `Harvest & Load Crops` (○ Pending)
    4. `Final Delivery & Payout Release` (○ Pending)
  - Driver info card with lorry plate and circular green direct phone call button.
  - Live route map with delivery progress bar (75%) and auto-payout departure disclaimer.
* **5.2 Start Harvesting Alert (`activity_start_harvesting.xml`)**:
  - Notification alert card with bell badge, vehicle arrival countdown (`45 minutes away`), crate packaging checklist, and `Acknowledge & Start Harvesting` button.
  - Live field status banner (`Zone A-12`).
* **5.3 Harvest Ready Trigger (`activity_harvest_ready.xml`)**:
  - `Status: Harvesting in Progress` chip.
  - Crates illustration card and readiness confirmation button (`(✓) Harvest Ready`).
  - Driver arrival card with expected time and phone call action.

### 6. Notifications & Feedback
* **6.1 Notifications Center (`activity_notifications.xml` & `NotificationsActivity.java`)**:
  - Unread/read styled alert cards with payout credit confirmations and listing approvals.
* **6.2 Rating & Feedback Screen (`activity_rating_feedback.xml` & `RatingFeedbackActivity.java`)**:
  - Transaction complete checkmark, order payout summary, 5-star interactive rating for Buyer and Transporter, and comment feedback box.

---

## 3. Backend & API Synchronization Completed

* **`ListingController` & `ListingService`**:
  - `POST /listings/create`: Create active farm listing.
  - `PUT /listings/{id}`: Update listing details.
  - `PUT /listings/{id}/status`: Transition states (`ACTIVE`, `DEACTIVATED`, `CLOSED`).
* **`OrderController` & `OrderService`**:
  - `GET /orders/farmer/{farmerId}`: Fetch all farmer orders.
  - `GET /orders/{orderId}`: Single order state for polling and navigation.
  - `PUT /orders/{orderId}/status`: Status transition pipeline (`PENDING_BUYER_APPROVAL` -> `ESCROW_SETUP` -> `HARVEST_LOCKED` -> `HARVEST_READY` -> `IN_TRANSIT` -> `DELIVERED` -> `CLOSED`).
  - `POST /orders/{orderId}/rate`: Submit dual-party ratings and comments.
* **`MarketController` & `MarketService`**:
  - `GET /market/centers`: Major Sri Lankan economic centers (Dambulla, Keppetipola, Meegoda, Narahenpita).
* **`DashboardService`**:
  - Dynamic time-based greeting generation and revenue analytics.

---

## 4. Verification & Testing

* **Java Compilation (`compileDebugJavaWithJavac`)**: `BUILD SUCCESSFUL`
* **APK Assembly (`assembleDebug`)**: `BUILD SUCCESSFUL` (12-17s build time, clean APK generated in `app/build/outputs/apk/debug/app-debug.apk`)
* **Encoding Integrity**: Verified 0 UTF-8 BOM issues across all XML layouts and Java sources.

---

## 5. Next Steps for Subsequent Milestone / Next Model

1. **Buyer & Transporter Views**:
   - Align Buyer mobile app screens with the requirements in `UI list of Buyer.docx`.
   - Implement the transporter coordination screen flow.
2. **WebSocket / Real-Time Order Updates**:
   - Replace 10-second polling handlers in order lifecycle activities with STOMP/WebSocket topics.
3. **End-to-End Live Environment Testing**:
   - Run end-to-end integration tests between mobile client and backend cloud instance (`https://harvesthubapp.tech/`).