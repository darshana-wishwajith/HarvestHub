/**
 * HarvestHub Database Structure & Seed Script for MongoDB
 * 
 * Target Database: harvesthub_db
 * Compatible with: mongosh, MongoDB Compass (Script / Aggregation), MongoDB Atlas
 * 
 * Usage via mongosh:
 *   mongosh "mongodb+srv://admin_havesthub_db_cluster:A3GBujkVHrebB8KK@harvesthubdbcluster.pwozoql.mongodb.net/harvesthub_db" seed_harvesthub_database.js
 */

const dbName = "harvesthub_db";
db = db.getSiblingDB(dbName);

print("==================================================");
print(" Starting HarvestHub Database Initialization...   ");
print(" Target DB: " + db.getName());
print("==================================================");

// -----------------------------------------------------------------------------
// 1. COLLECTION: users
// -----------------------------------------------------------------------------
print("\n[1/11] Initializing collection: users");
db.users.drop();

// Schema validation and index setup
db.createCollection("users", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["fullName", "mobileNumber", "nic", "email", "password", "role", "farmLocation"],
      properties: {
        _id: { bsonType: "string" },
        fullName: { bsonType: "string", description: "Full legal name" },
        mobileNumber: { bsonType: "string", pattern: "^\\d{10}$", description: "10-digit Sri Lankan phone number" },
        phoneNumber: { bsonType: "string" },
        nic: { bsonType: "string", description: "National Identity Card number" },
        email: { bsonType: "string", pattern: "^.+@.+\\..+$", description: "Valid email address" },
        password: { bsonType: "string", description: "BCrypt encrypted hash" },
        role: { enum: ["FARMER", "BUYER", "OFFICER", "TRANSPORTER", "ADMIN"], description: "System security role" },
        farmLocation: {
          bsonType: "object",
          required: ["address", "latitude", "longitude"],
          properties: {
            address: { bsonType: "string" },
            latitude: { bsonType: ["double", "int"] },
            longitude: { bsonType: ["double", "int"] }
          }
        },
        profilePhotoUrl: { bsonType: "string" },
        passwordLastUpdated: { bsonType: "string" },
        resetPasswordToken: { bsonType: ["string", "null"] },
        resetPasswordTokenExpiry: { bsonType: ["date", "null"] }
      }
    }
  }
});

db.users.createIndex({ email: 1 }, { unique: true });
db.users.createIndex({ mobileNumber: 1 }, { unique: true });
db.users.createIndex({ nic: 1 }, { unique: true });
db.users.createIndex({ role: 1 });

// BCrypt hash for "password123": $2a$10$7zB3cWk.4kR12Z2g3FfGMezYyvF...
// BCrypt hash for "admin123": $2a$10$wE99gT3W8n9Wz...
// Pre-computed BCrypt hashes compatible with Spring Security BCryptPasswordEncoder:
const defaultHash = "$2a$10$rDkPvvafV8KQwvK0Fp21o.uGZ8w7jJb6vCvyj/GgB62Q888OqU0te"; // password123
const adminHash = "$2a$10$sX8.9j72Kk2Q1gI290uX6.YyA3P5Gv8E9e3jI4v2F9Q721lM8K9eq";   // admin123

db.users.insertMany([
  {
    _id: "user_farmer_kamal",
    fullName: "Kamal Perera",
    mobileNumber: "0771234567",
    phoneNumber: "0771234567",
    nic: "852145659V",
    email: "kamal.farmer@harvesthub.lk",
    password: defaultHash,
    role: "FARMER",
    farmLocation: {
      address: "No 42, Matale Road, Dambulla",
      latitude: 7.8731,
      longitude: 80.7718
    },
    profilePhotoUrl: "/uploads/photos/kamal_profile.jpg",
    passwordLastUpdated: "2026-09-01T10:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_farmer_sunil",
    fullName: "Sunil Shantha",
    mobileNumber: "0712345678",
    phoneNumber: "0712345678",
    nic: "791245789V",
    email: "sunil.shantha@harvesthub.lk",
    password: defaultHash,
    role: "FARMER",
    farmLocation: {
      address: "Blackpool Farm, Nuwara Eliya",
      latitude: 6.9497,
      longitude: 80.7891
    },
    profilePhotoUrl: "/uploads/photos/sunil_profile.jpg",
    passwordLastUpdated: "2026-09-05T11:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_farmer_chaminda",
    fullName: "Chaminda Bandara",
    mobileNumber: "0753344556",
    phoneNumber: "0753344556",
    nic: "911234567V",
    email: "chaminda.bandara@harvesthub.lk",
    password: defaultHash,
    role: "FARMER",
    farmLocation: {
      address: "Bakamuna Road, Polonnaruwa",
      latitude: 7.9403,
      longitude: 81.0188
    },
    profilePhotoUrl: "/uploads/photos/chaminda_profile.jpg",
    passwordLastUpdated: "2026-09-10T09:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_buyer_keells",
    fullName: "Keells Super Procurement",
    mobileNumber: "0112303500",
    phoneNumber: "0112303500",
    nic: "198512345678",
    email: "procurement@keells.lk",
    password: defaultHash,
    role: "BUYER",
    farmLocation: {
      address: "Keells Distribution Centre, Kerawalapitiya, Wattala",
      latitude: 7.0025,
      longitude: 79.8872
    },
    profilePhotoUrl: "/uploads/photos/keells_buyer.jpg",
    passwordLastUpdated: "2026-08-15T08:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_buyer_cargills",
    fullName: "Cargills Agrifoods",
    mobileNumber: "0112427777",
    phoneNumber: "0112427777",
    nic: "198098765432",
    email: "agri.procurement@cargillsceylon.com",
    password: defaultHash,
    role: "BUYER",
    farmLocation: {
      address: "Cargills Central Logistics Hub, Katunayake",
      latitude: 7.1650,
      longitude: 79.8820
    },
    profilePhotoUrl: "/uploads/photos/cargills_buyer.jpg",
    passwordLastUpdated: "2026-08-20T08:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_officer_anura",
    fullName: "Dr. Anura Senanayake",
    mobileNumber: "0777123987",
    phoneNumber: "0777123987",
    nic: "721568945V",
    email: "anura.officer@doa.gov.lk",
    password: defaultHash,
    role: "OFFICER",
    farmLocation: {
      address: "Department of Agriculture, Central Office, Peradeniya",
      latitude: 7.2618,
      longitude: 80.5975
    },
    profilePhotoUrl: "/uploads/photos/officer_anura.jpg",
    passwordLastUpdated: "2026-08-10T14:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_officer_dilani",
    fullName: "Dilani Silva",
    mobileNumber: "0719876543",
    phoneNumber: "0719876543",
    nic: "887654321V",
    email: "dilani.silva@doa.gov.lk",
    password: defaultHash,
    role: "OFFICER",
    farmLocation: {
      address: "Agrarian Services Centre, Dambulla",
      latitude: 7.8680,
      longitude: 80.7680
    },
    profilePhotoUrl: "/uploads/photos/officer_dilani.jpg",
    passwordLastUpdated: "2026-08-12T09:30:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_transporter_nimal",
    fullName: "Nimal Fernando",
    mobileNumber: "0718899221",
    phoneNumber: "0718899221",
    nic: "782345678V",
    email: "nimal.logistics@harvesthub.lk",
    password: defaultHash,
    role: "TRANSPORTER",
    farmLocation: {
      address: "Nimal Logistics Yard, Kurunegala",
      latitude: 7.4863,
      longitude: 80.3623
    },
    profilePhotoUrl: "/uploads/photos/driver_nimal.jpg",
    passwordLastUpdated: "2026-08-25T16:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  },
  {
    _id: "user_admin",
    fullName: "HarvestHub Administrator",
    mobileNumber: "0700000000",
    phoneNumber: "0700000000",
    nic: "900000000V",
    email: "admin@harvesthub.lk",
    password: adminHash,
    role: "ADMIN",
    farmLocation: {
      address: "HarvestHub HQ, Colombo 03",
      latitude: 6.9034,
      longitude: 79.8540
    },
    profilePhotoUrl: "/uploads/photos/admin.jpg",
    passwordLastUpdated: "2026-08-01T00:00:00",
    resetPasswordToken: null,
    resetPasswordTokenExpiry: null
  }
]);
print("  ✓ Inserted " + db.users.countDocuments() + " users.");

// -----------------------------------------------------------------------------
// 2. COLLECTION: crop_batches
// -----------------------------------------------------------------------------
print("\n[2/11] Initializing collection: crop_batches");
db.crop_batches.drop();
db.crop_batches.createIndex({ farmerId: 1, cropType: 1 });
db.crop_batches.createIndex({ batchNumber: 1 }, { unique: true });

db.crop_batches.insertMany([
  {
    _id: "batch_kamal_onion_01",
    batchNumber: "BATCH-2026-ONION-01",
    farmerId: "user_farmer_kamal",
    cropType: "Big Onions",
    availableQuantity: 2500.0,
    isVerified: true,
    createdDate: "2026-09-20T08:30:00"
  },
  {
    _id: "batch_kamal_carrot_02",
    batchNumber: "BATCH-2026-CARROT-02",
    farmerId: "user_farmer_kamal",
    cropType: "Carrots",
    availableQuantity: 1200.0,
    isVerified: true,
    createdDate: "2026-09-22T09:15:00"
  },
  {
    _id: "batch_kamal_paddy_03",
    batchNumber: "BATCH-2026-PADDY-03",
    farmerId: "user_farmer_kamal",
    cropType: "Nadu Rice",
    availableQuantity: 5000.0,
    isVerified: true,
    createdDate: "2026-09-25T11:00:00"
  },
  {
    _id: "batch_sunil_potato_01",
    batchNumber: "BATCH-2026-POTATO-01",
    farmerId: "user_farmer_sunil",
    cropType: "Potatoes",
    availableQuantity: 3500.0,
    isVerified: true,
    createdDate: "2026-09-24T10:00:00"
  },
  {
    _id: "batch_sunil_leeks_02",
    batchNumber: "BATCH-2026-LEEKS-02",
    farmerId: "user_farmer_sunil",
    cropType: "Leeks",
    availableQuantity: 1800.0,
    isVerified: false,
    createdDate: "2026-09-27T14:00:00"
  },
  {
    _id: "batch_chaminda_samba_01",
    batchNumber: "BATCH-2026-SAMBA-01",
    farmerId: "user_farmer_chaminda",
    cropType: "Keeri Samba",
    availableQuantity: 8000.0,
    isVerified: true,
    createdDate: "2026-09-21T07:45:00"
  }
]);
print("  ✓ Inserted " + db.crop_batches.countDocuments() + " crop batches.");

// -----------------------------------------------------------------------------
// 3. COLLECTION: listings
// -----------------------------------------------------------------------------
print("\n[3/11] Initializing collection: listings");
db.listings.drop();
db.listings.createIndex({ farmerId: 1, status: 1 });
db.listings.createIndex({ cropName: 1 });

db.listings.insertMany([
  {
    _id: "listing_kamal_onion_01",
    farmerId: "user_farmer_kamal",
    cropName: "Big Onions",
    location: "Dambulla",
    quantity: "2,500 kg",
    price: "Rs. 250/kg",
    createdDate: "2026-09-26T08:00:00",
    updatedTime: "2026-09-28T09:30:00",
    status: "ACTIVE",
    buyerName: null,
    quantityKg: "2500",
    totalValue: "Rs. 625,000",
    finalPricePerKg: "250"
  },
  {
    _id: "listing_kamal_carrot_02",
    farmerId: "user_farmer_kamal",
    cropName: "Fresh Carrots",
    location: "Dambulla / Nuwara Eliya Road",
    quantity: "1,200 kg",
    price: "Rs. 280/kg",
    createdDate: "2026-09-27T10:15:00",
    updatedTime: "2026-09-28T10:00:00",
    status: "ACTIVE",
    buyerName: null,
    quantityKg: "1200",
    totalValue: "Rs. 336,000",
    finalPricePerKg: "280"
  },
  {
    _id: "listing_kamal_hist_402",
    farmerId: "user_farmer_kamal",
    cropName: "Big Onions (Grade A)",
    location: "Dambulla",
    quantity: "500 kg",
    price: "Rs. 250/kg",
    createdDate: "2026-09-15T09:00:00",
    updatedTime: "2026-09-18T17:00:00",
    status: "DELIVERED",
    buyerName: "Keells Super Procurement",
    quantityKg: "500",
    totalValue: "Rs. 125,000",
    finalPricePerKg: "250"
  },
  {
    _id: "listing_kamal_hist_398",
    farmerId: "user_farmer_kamal",
    cropName: "Red Tomatoes",
    location: "Dambulla",
    quantity: "250 kg",
    price: "Rs. 170/kg",
    createdDate: "2026-09-10T11:20:00",
    updatedTime: "2026-09-12T15:00:00",
    status: "DELIVERED",
    buyerName: "Cargills Agrifoods",
    quantityKg: "250",
    totalValue: "Rs. 42,500",
    finalPricePerKg: "170"
  },
  {
    _id: "listing_sunil_potato_01",
    farmerId: "user_farmer_sunil",
    cropName: "Nuwara Eliya Potatoes",
    location: "Nuwara Eliya",
    quantity: "3,500 kg",
    price: "Rs. 320/kg",
    createdDate: "2026-09-25T14:00:00",
    updatedTime: "2026-09-28T08:00:00",
    status: "ACTIVE",
    buyerName: null,
    quantityKg: "3500",
    totalValue: "Rs. 1,120,000",
    finalPricePerKg: "320"
  },
  {
    _id: "listing_chaminda_rice_01",
    farmerId: "user_farmer_chaminda",
    cropName: "Keeri Samba Rice",
    location: "Polonnaruwa",
    quantity: "8,000 kg",
    price: "Rs. 230/kg",
    createdDate: "2026-09-22T08:30:00",
    updatedTime: "2026-09-28T09:00:00",
    status: "ACTIVE",
    buyerName: null,
    quantityKg: "8000",
    totalValue: "Rs. 1,840,000",
    finalPricePerKg: "230"
  }
]);
print("  ✓ Inserted " + db.listings.countDocuments() + " marketplace listings.");

// -----------------------------------------------------------------------------
// 4. COLLECTION: wanted_ads
// -----------------------------------------------------------------------------
print("\n[4/11] Initializing collection: wanted_ads");
db.wanted_ads.drop();
db.wanted_ads.createIndex({ requestId: 1 }, { unique: true });
db.wanted_ads.createIndex({ cropName: 1 });
db.wanted_ads.createIndex({ buyerId: 1 });

db.wanted_ads.insertMany([
  {
    _id: "REQ-BO-092",
    requestId: "REQ-BO-092",
    cropName: "Big Onions",
    location: "Dambulla Economic Center",
    requiredQuantityKg: 5000.0,
    targetPricePerKg: 245.0,
    isUrgent: true,
    buyerId: "user_buyer_keells",
    createdDate: "2026-09-27T08:00:00"
  },
  {
    _id: "REQ-POT-104",
    requestId: "REQ-POT-104",
    cropName: "Potatoes",
    location: "Meegoda Dedicated Economic Centre",
    requiredQuantityKg: 3000.0,
    targetPricePerKg: 310.0,
    isUrgent: true,
    buyerId: "user_buyer_cargills",
    createdDate: "2026-09-26T14:30:00"
  },
  {
    _id: "REQ-TOM-118",
    requestId: "REQ-TOM-118",
    cropName: "Tomatoes",
    location: "Keppetipola Economic Center",
    requiredQuantityKg: 2000.0,
    targetPricePerKg: 185.0,
    isUrgent: false,
    buyerId: "user_buyer_keells",
    createdDate: "2026-09-28T07:15:00"
  },
  {
    _id: "REQ-CAB-125",
    requestId: "REQ-CAB-125",
    cropName: "Cabbage",
    location: "Narahenpita Dedicated Economic Centre",
    requiredQuantityKg: 1500.0,
    targetPricePerKg: 160.0,
    isUrgent: false,
    buyerId: "user_buyer_cargills",
    createdDate: "2026-09-28T08:30:00"
  }
]);
print("  ✓ Inserted " + db.wanted_ads.countDocuments() + " buyer wanted ads.");

// -----------------------------------------------------------------------------
// 5. COLLECTION: supply_offers
// -----------------------------------------------------------------------------
print("\n[5/11] Initializing collection: supply_offers");
db.supply_offers.drop();
db.supply_offers.createIndex({ requestId: 1 });
db.supply_offers.createIndex({ farmerId: 1 });

db.supply_offers.insertMany([
  {
    _id: "offer_kamal_onion_01",
    requestId: "REQ-BO-092",
    batchId: "batch_kamal_onion_01",
    farmerId: "user_farmer_kamal",
    quantity: 2000.0,
    pricePerKg: 245.0,
    status: "ACCEPTED",
    createdDate: "2026-09-27T10:00:00"
  },
  {
    _id: "offer_sunil_potato_01",
    requestId: "REQ-POT-104",
    batchId: "batch_sunil_potato_01",
    farmerId: "user_farmer_sunil",
    quantity: 3000.0,
    pricePerKg: 310.0,
    status: "PENDING",
    createdDate: "2026-09-27T16:00:00"
  }
]);
print("  ✓ Inserted " + db.supply_offers.countDocuments() + " supply offers.");

// -----------------------------------------------------------------------------
// 6. COLLECTION: orders
// -----------------------------------------------------------------------------
print("\n[6/11] Initializing collection: orders");
db.orders.drop();
db.orders.createIndex({ orderId: 1 }, { unique: true });
db.orders.createIndex({ farmerId: 1, status: 1 });
db.orders.createIndex({ buyerId: 1 });
db.orders.createIndex({ transporterId: 1, status: 1 });

db.orders.insertMany([
  {
    _id: "ORD-892",
    orderId: "ORD-892",
    farmerId: "user_farmer_kamal",
    buyerId: "user_buyer_keells",
    listingId: "listing_kamal_onion_01",
    cropName: "Big Onions",
    farmerName: "Kamal Perera",
    buyerName: "Keells Super Procurement",
    transporterId: "user_transporter_nimal",
    quantity: 1500.0,
    pricePerKg: 250.0,
    totalAmount: 375000.0,
    escrowStatus: "ESCROW_LOCKED",
    deliveryDate: "2026-09-30",
    pickupDate: "2026-09-29 08:30 AM",
    actualDeliveryDate: null,
    status: "HARVEST_LOCKED",
    createdDate: "2026-09-28T06:00:00",
    rating: null,
    ratingComment: null,
    statusHistory: [
      {
        status: "PENDING_BUYER_APPROVAL",
        timestamp: "2026-09-28T06:00:00",
        note: "Order placed by Keells Super Procurement"
      },
      {
        status: "ESCROW_SETUP",
        timestamp: "2026-09-28T06:30:00",
        note: "Buyer payment of LKR 375,000 cleared into Escrow account"
      },
      {
        status: "HARVEST_LOCKED",
        timestamp: "2026-09-28T07:15:00",
        note: "Transporter Nimal Logistics assigned, vehicle WP-LF-4512 en route. Harvest locked."
      }
    ],
    transportEvents: []
  },
  {
    _id: "ORD-402",
    orderId: "ORD-402",
    farmerId: "user_farmer_kamal",
    buyerId: "user_buyer_keells",
    listingId: "listing_kamal_hist_402",
    cropName: "Big Onions (Grade A)",
    farmerName: "Kamal Perera",
    buyerName: "Keells Super Procurement",
    transporterId: "user_transporter_nimal",
    quantity: 500.0,
    pricePerKg: 250.0,
    totalAmount: 125000.0,
    escrowStatus: "RELEASED",
    deliveryDate: "2026-09-18",
    pickupDate: "2026-09-17 07:00 AM",
    actualDeliveryDate: "2026-09-18 04:30 PM",
    status: "DELIVERED",
    createdDate: "2026-09-15T09:00:00",
    rating: 5,
    ratingComment: "Excellent crop quality, fresh Big Onions properly sorted in standard 50kg crates.",
    statusHistory: [
      {
        status: "PENDING_BUYER_APPROVAL",
        timestamp: "2026-09-15T09:00:00",
        note: "Order initiated"
      },
      {
        status: "ESCROW_SETUP",
        timestamp: "2026-09-15T09:30:00",
        note: "Escrow funded: LKR 125,000"
      },
      {
        status: "DELIVERED",
        timestamp: "2026-09-18T16:30:00",
        note: "Delivered and verified at Keells Distribution Hub"
      }
    ]
  },
  {
    _id: "ORD-398",
    orderId: "ORD-398",
    farmerId: "user_farmer_kamal",
    buyerId: "user_buyer_cargills",
    listingId: "listing_kamal_hist_398",
    cropName: "Red Tomatoes",
    farmerName: "Kamal Perera",
    buyerName: "Cargills Agrifoods",
    transporterId: "user_transporter_nimal",
    quantity: 250.0,
    pricePerKg: 170.0,
    totalAmount: 42500.0,
    escrowStatus: "RELEASED",
    deliveryDate: "2026-09-12",
    pickupDate: "2026-09-11 08:00 AM",
    actualDeliveryDate: "2026-09-12 02:10 PM",
    status: "DELIVERED",
    createdDate: "2026-09-10T11:20:00",
    rating: 5,
    ratingComment: "Great ripeness, undamaged transport and timely delivery.",
    statusHistory: [
      {
        status: "DELIVERED",
        timestamp: "2026-09-12T14:10:00",
        note: "Delivered to Cargills Agrifoods DC"
      }
    ]
  }
]);
print("  ✓ Inserted " + db.orders.countDocuments() + " orders.");

// -----------------------------------------------------------------------------
// 7. COLLECTION: wallet_summaries
// -----------------------------------------------------------------------------
print("\n[7/11] Initializing collection: wallet_summaries");
db.wallet_summaries.drop();
db.wallet_summaries.createIndex({ farmerId: 1 }, { unique: true });

db.wallet_summaries.insertMany([
  {
    _id: "wallet_kamal",
    farmerId: "user_farmer_kamal",
    availableBalance: "Rs. 125,000.00",
    trustScore: "4.8",
    emptyTripPenalties: "Rs. 0.00"
  },
  {
    _id: "wallet_sunil",
    farmerId: "user_farmer_sunil",
    availableBalance: "Rs. 84,000.00",
    trustScore: "4.6",
    emptyTripPenalties: "Rs. 0.00"
  },
  {
    _id: "wallet_chaminda",
    farmerId: "user_farmer_chaminda",
    availableBalance: "Rs. 210,000.00",
    trustScore: "4.9",
    emptyTripPenalties: "Rs. 0.00"
  }
]);
print("  ✓ Inserted " + db.wallet_summaries.countDocuments() + " wallet summaries.");

// -----------------------------------------------------------------------------
// 8. COLLECTION: transactions
// -----------------------------------------------------------------------------
print("\n[8/11] Initializing collection: transactions");
db.transactions.drop();
db.transactions.createIndex({ farmerId: 1, timestamp: -1 });

db.transactions.insertMany([
  {
    _id: "tx_kamal_402",
    farmerId: "user_farmer_kamal",
    title: "Order #402 Settled - Big Onions",
    timestamp: "2026-09-18 17:00",
    amount: "Rs. 125,000.00",
    isCredit: true
  },
  {
    _id: "tx_kamal_398",
    farmerId: "user_farmer_kamal",
    title: "Order #398 Settled - Red Tomatoes",
    timestamp: "2026-09-12 15:00",
    amount: "Rs. 42,500.00",
    isCredit: true
  },
  {
    _id: "tx_kamal_advance",
    farmerId: "user_farmer_kamal",
    title: "Escrow Advance Reserved - Order #ORD-892",
    timestamp: "2026-09-28 07:30",
    amount: "Rs. 50,000.00",
    isCredit: true
  }
]);
print("  ✓ Inserted " + db.transactions.countDocuments() + " transactions.");

// -----------------------------------------------------------------------------
// 9. COLLECTION: notifications
// -----------------------------------------------------------------------------
print("\n[9/11] Initializing collection: notifications");
db.notifications.drop();
db.notifications.createIndex({ farmerId: 1, isRead: 1 });

db.notifications.insertMany([
  {
    _id: "notif_01",
    farmerId: "user_farmer_kamal",
    type: "ESCROW",
    title: "Funds in Escrow: Order #ORD-892",
    body: "LKR 375,000 has been secured in escrow by Keells Super. Transporter Nimal Logistics is arriving in 45 minutes.",
    timestamp: "2026-09-28 07:15 AM",
    isRead: false
  },
  {
    _id: "notif_02",
    farmerId: "user_farmer_kamal",
    type: "VERIFICATION",
    title: "Listing Approved",
    body: "Officer Dilani Silva has verified and approved your Big Onions batch BATCH-2026-ONION-01.",
    timestamp: "2026-09-26 10:30 AM",
    isRead: true
  },
  {
    _id: "notif_03",
    farmerId: "user_farmer_kamal",
    type: "PAYMENT",
    title: "Payout Released: Order #402",
    body: "Payment of LKR 125,000 has been credited to your HarvestHub wallet.",
    timestamp: "2026-09-18 05:00 PM",
    isRead: true
  }
]);
print("  ✓ Inserted " + db.notifications.countDocuments() + " notifications.");

// -----------------------------------------------------------------------------
// 10. COLLECTION: chat_messages
// -----------------------------------------------------------------------------
print("\n[10/11] Initializing collection: chat_messages");
db.chat_messages.drop();
db.chat_messages.createIndex({ orderId: 1, timestamp: 1 });

db.chat_messages.insertMany([
  {
    _id: "chat_01",
    orderId: "ORD-892",
    senderId: "user_transporter_nimal",
    text: "Ayubowan Kamal aiya, I am loading near Dambulla clock tower now. Will arrive at your field in 40 minutes.",
    timestamp: "2026-09-28 07:20 AM",
    isSentByMe: false,
    isRead: true
  },
  {
    _id: "chat_02",
    orderId: "ORD-892",
    senderId: "user_farmer_kamal",
    text: "Ayubowan Nimal, the 30 crates are harvested and packed under the shed. The road is clear for the lorry.",
    timestamp: "2026-09-28 07:25 AM",
    isSentByMe: true,
    isRead: true
  },
  {
    _id: "chat_03",
    orderId: "ORD-892",
    senderId: "user_buyer_keells",
    text: "Kamal, please make sure crates have proper aeration tags. We have cleared the receiving bay at Wattala DC.",
    timestamp: "2026-09-28 07:35 AM",
    isSentByMe: false,
    isRead: false
  }
]);
print("  ✓ Inserted " + db.chat_messages.countDocuments() + " chat messages.");

// -----------------------------------------------------------------------------
// 11. COLLECTION: market_centers (Economic Centers Reference Data)
// -----------------------------------------------------------------------------
print("\n[11/11] Initializing collection: market_centers");
db.market_centers.drop();
db.market_centers.createIndex({ name: 1 }, { unique: true });

db.market_centers.insertMany([
  {
    _id: "center_dambulla",
    name: "Dambulla Dedicated Economic Centre",
    location: "Dambulla",
    district: "Matale",
    supplyLevel: "High",
    demandLevel: "High",
    latitude: 7.8742,
    longitude: 80.6511
  },
  {
    _id: "center_keppetipola",
    name: "Keppetipola Dedicated Economic Centre",
    location: "Keppetipola",
    district: "Badulla",
    supplyLevel: "Medium",
    demandLevel: "High",
    latitude: 6.9038,
    longitude: 80.8523
  },
  {
    _id: "center_meegoda",
    name: "Meegoda Dedicated Economic Centre",
    location: "Meegoda",
    district: "Colombo",
    supplyLevel: "High",
    demandLevel: "Medium",
    latitude: 6.8488,
    longitude: 80.0558
  },
  {
    _id: "center_narahenpita",
    name: "Narahenpita Dedicated Economic Centre",
    location: "Narahenpita",
    district: "Colombo",
    supplyLevel: "Medium",
    demandLevel: "Low",
    latitude: 6.8920,
    longitude: 79.8828
  },
  {
    _id: "center_nuwaraeliya",
    name: "Nuwara Eliya Wholesale Market",
    location: "Nuwara Eliya",
    district: "Nuwara Eliya",
    supplyLevel: "High",
    demandLevel: "High",
    latitude: 6.9708,
    longitude: 80.7829
  },
  {
    _id: "center_thambuththegama",
    name: "Thambuththegama Dedicated Economic Centre",
    location: "Thambuththegama",
    district: "Anuradhapura",
    supplyLevel: "High",
    demandLevel: "Medium",
    latitude: 8.1500,
    longitude: 80.2833
  }
]);
print("  ✓ Inserted " + db.market_centers.countDocuments() + " market centers.");

print("\n==================================================");
print(" HarvestHub Database Initialization Complete!   ");
print(" Collections Created & Populated: 11              ");
print("==================================================");
