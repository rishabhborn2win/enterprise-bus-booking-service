# Bus System Project: Search and Administration Service

This repository contains the backend services for a high-performance Bus Search and Administration system. The architecture uses **MySQL** as the transactional system-of-record and **Elasticsearch** (via Elastic Cloud) for fast, scalable search capabilities.

The core feature demonstrated is the accurate, real-time calculation of **segment-based seat availability**, ensuring a seat is blocked only for the specific segments where a booking overlap exists.

***

## 1. Architecture and Technologies 🏗️

The project utilizes a modern **Spring Boot 3.x** stack to manage the dual-database structure:

| Component | Technology | Role / Integration |
| :--- | :--- | :--- |
| **Backend Framework** | **Spring Boot 3.x (Java 17+)** | Manages REST APIs, services, and transactions. |
| **Source of Truth** | **MySQL 8.0+ (JPA)** | Stores all master data (`Bus`, `Route`, `Stop`) and transactional records (`Booking`, `Schedule`). |
| **Search Engine** | **Elasticsearch 9.1.4 (Elastic Cloud)** | Search index for fast schedule filtering and pagination. |
| **Client** | **Spring Data Elasticsearch 5.x** | Connects securely to the remote Cloud instance using **API Key Authentication**. |
| **Search Logic** | **Segment Overlap Logic** | Calculates available seats by comparing the requested segment (e.g., Pune to Mysore) against all confirmed bookings on the same bus run. |
| **API Docs** | **SpringDoc/Swagger UI** | Provides interactive documentation for all endpoints. |

***

## 2. Local Setup Guide for Interviewer 🚀

To run this application, you must configure the connection to your external services.

### 2.1. Prerequisites

1.  **Java JDK 17+**
2.  **Maven 3.6+**
3.  **MySQL Server (8.0+)**
4.  **Elastic Cloud Deployment:** Access to the deployed Elasticsearch 9.1.4 instance, its **Cloud ID**, and a generated **API Key**.

### 2.2. MySQL Database Setup

1.  **Create Database:**
    ```sql
    CREATE DATABASE IF NOT EXISTS bus_system_db;
    ```
2.  **Load Schema and Sample Data:** Run the comprehensive SQL script (`sample_data.sql` with its extensions) to populate the necessary tables (`Bus`, `Route`, `Schedule`, `Schedule_Stop`, and pre-configured **`Booking_Seat`** records for overlap testing).
    ```bash
    mysql -u root -p bus_system_db < path/to/sample_data.sql
    ```

### 2.3. Configure Secure Elasticsearch Connection

The application uses a **custom configuration class** to force a secure connection over HTTPS, bypassing Spring Boot's default `localhost:9200` fallback.

1.  **Locate Configuration File:** Open `src/main/resources/application.properties` (or `application.yml`).
2.  **Insert Secure Credentials:** The client configuration requires two specific values under custom keys to prevent auto-configuration conflicts:

    ```properties
    # --- CRITICAL ELASTIC CLOUD CONFIGURATION ---

    # These properties are read by the custom ElasticsearchClientConfig.java via @Value
    
    # 1. Cloud ID: The full deployment identifier
    es.cloud.id=[YOUR_CLOUD_ID_HERE] 

    # 2. API Key: The Base64-encoded API key for authentication
    es.api-key=[YOUR_API_KEY_HERE] 

    # Example of other standard properties (Ensure MySQL is defined here too)
    spring.elasticsearch.connection-timeout=15s
    ```

### 2.4. Run the Application

1.  **Build and Clean Dependencies:**
    ```bash
    mvn clean install -U
    ```
2.  **Run:**
    ```bash
    mvn spring-boot:run
    ```

The service will start on `http://localhost:8080`.

***

## 3. Testing Workflow and Key Endpoints 🎯

Access the Swagger UI for interactive testing: **`http://localhost:8080/swagger-ui.html`**

### Step 3.1: Data Initialization (Synchronization)

The first step is always to synchronize MySQL data to the Elasticsearch index.

| Action | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Initial Sync** | **POST** | `/api/admin/sync/full` | **CRITICAL:** Executes the `SyncService` to read all current schedules from MySQL and index them in Elastic Cloud. **Run this immediately.** |

### Step 3.2: Testing Segment Search Logic (The Core Feature)

Use Schedule **301** (Mumbai **11** $\to$ Pune **10** $\to$ Bangalore **12**) to test the segment availability. The sample data has seats blocked to create specific overlaps.

| Test Query Segment | Search Stop IDs | Logic Verified |
| :--- | :--- | :--- |
| **Full Trip** | $11 \to 12$ | **Full Overlap Check:** Blocks seats booked for any sub-segment. |
| **Intermediate Segment** | $11 \to 10$ | **Partial Overlap:** Verifies that a seat booked for $10 \to 12$ is correctly shown as **AVAILABLE** for the $11 \to 10$ query. |
| **Tail Segment** | $10 \to 12$ | **Partial Overlap:** Verifies that a seat booked for $11 \to 10$ is correctly shown as **AVAILABLE** for the $10 \to 12$ query. |

This demonstrates that the search query uses **Elasticsearch** for filtering and the **MySQL/Java** layer for high-integrity segment validation.