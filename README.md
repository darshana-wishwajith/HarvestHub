# HarvestHub: Integrated Agricultural & Logistics System

[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-green)](https://spring.io/projects/spring-boot)
[![Project Status](https://img.shields.io/badge/MySQL-8-blue)](#-project-status-increment-01)

---

## 🌾 Overview

**HarvestHub** is a digital ecosystem designed to bridge the **"Trust Gap"** in Sri Lanka's agricultural supply chain. It connects **Farmers**, **Buyers**, **Agricultural Officers**, and **Transporters** through a secure, verified, and transparent marketplace.

### Key Solutions

* **Market Insights** – Helping farmers align production with real-world demand
* **Quality Assurance** – Agricultural Officers verify crop listings using GPS-tagged images and videos
* **Payment-First Policy** – Protecting farmers by initiating harvests only after buyer commitment
* **Logistics Automation** – Transport fee calculation based on crop weight and travel distance

---

## 🛠️ Technical Stack

The system adopts a **multi-tier architecture** to ensure scalability, performance, and security.

* **Backend:** Spring Boot (REST API), Spring Security, JWT Authentication
* **Desktop Application (Agricultural Officers):** JavaFX + Gluon Scene Builder
* **Mobile Application (Farmers & Buyers):** Android (Java + XML), Retrofit
* **Database:** MySQL 8.0
* **External Services:** Google Maps API (GPS validation & logistics calculations)

---

## 📂 Project Structure

The repository follows a **monorepo structure** for streamlined collaboration:

```text
HarvestHub/
├── harvesthub-backend/    # Spring Boot REST API
├── harvesthub-desktop/    # JavaFX Officer/Admin Panel
└── harvesthub-android/    # Farmer & Buyer Android App
```

---

## 🚀 Getting Started

### Prerequisites

* **JDK 21+**
* **MySQL Server 8.0+**
* **IntelliJ IDEA** (Ultimate recommended)
* **Android Studio**

---

### Setup Instructions

1. **Clone the Repository**

```bash
git clone https://github.com/YourGroup/HarvestHub.git
```

2. **Database Setup**

* Open MySQL Workbench
* Create a schema named `harvesthub`
* Run the provided `schema.sql` file

3. **Backend Setup**

* Navigate to `harvesthub-backend/src/main/resources/application.properties`
* Update your MySQL credentials
* Start the backend server:

```bash
./mvnw spring-boot:run
```

4. **Desktop Application**

* Open `harvesthub-desktop` in IntelliJ IDEA
* Run the main application class

5. **Mobile Application**

* Open `harvesthub-android` in Android Studio
* Deploy to an emulator or physical device

---

