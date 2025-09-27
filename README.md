# Bus System Project: Search, Booking, and Administration Service

This repository contains the backend services for a high-performance Bus Search, Booking, and Administration system. The architecture relies on a crucial **three-layer data strategy** to ensure both speed and transactional integrity.
Extensible to User Service (but not included here).

| Layer | Technology | Role / Integration |
| :--- | :--- | :--- |
| **Transactional DB** | **MySQL 8.0+ (JPA)** | Stores all master data and is the **Source of Truth** for bookings. |
| **Search Engine** | **Elasticsearch 9.1.4 (Elastic Cloud)** | Provides high-speed, paginated search and filtering for schedules. |
| **Concurrency/Cache** | **Redis (Distributed Lock)** | Manages **distributed locks** on seats during the booking process to prevent race conditions. |

***

## 1. Key Features & Goals (FR/NFR) 💡

This project showcases solutions to complex, real-world constraints in transportation booking systems:

### Functional Requirements (What the System Does)

| Area | Requirement Solved |
| :--- | :--- |
| **Concurrency Control** | **Distributed Locking (Redis):** Uses Redis to acquire an atomic lock on selected seats, guaranteeing that only one user can proceed with booking a seat during the checkout phase. |
| **Availability** | **Segmented Seat Availability:** Calculates available seats in real-time by checking MySQL booking records for time-overlap against the requested segment. |
| **Search Logic** | **Segmented Search:** Queries Elasticsearch to find schedules that cover any **intermediate segment** requested by the user. |
| **Data Integrity** | **Real-Time Bus Allocation:** Conflict checks prevent double-booking of a single bus based on its full trip termination time (final stop arrival). |
| **Schedule Management** | **Transactional Creation:** Creates a single **Schedule** along with all its **intermediate stops** in an atomic MySQL transaction. |

### Non-Functional Requirements (How Well it Does It)

| Area | Goal Achieved |
| :--- | :--- |
| **Performance** | **High-Speed Search:** Filtering and pagination logic is delegated entirely to **Elasticsearch**. |
| **Data Consistency** | **Transactional Safety:** All administrative and booking-prep operations adhere to ACID properties. |
| **Security** | **Cloud Connectivity:** Secure connection to Elastic Cloud enforced using **HTTPS** and **API Key** authentication. |

***

## 2. Local Setup Guide and Execution 🚀

The local environment (MySQL and Redis) is automated using the `parent_setup.sh` script.

### 2.1. Prerequisites

1.  **Java JDK 17+**
2.  **Maven 3.6+**
3.  **Docker Engine:** Must be running for automatic provisioning of MySQL/Redis if they are not running locally.
4.  **Elastic Cloud Deployment:** Access to the deployed Elasticsearch 9.1.4 instance, its **Cloud ID**, and a generated **API Key**.

### 2.2. Automated Setup and Launch

1.  **Ensure Script and Data are Present:** Make sure `parent_setup.sh` and `sample_data.sql` are in the root directory.
2.  **Grant Execution Permission:**
    ```bash
    chmod +x parent_setup.sh
    ```
3.  **Run the Setup Script:** The script will prompt for MySQL credentials, provision services (MySQL/Redis), and then pause for the configuration step.
    ```bash
    ./parent_setup.sh
    ```

### 2.3. Configuration Update (Manual Step)

When the script pauses, you must update `src/main/resources/application.properties` (or `application.yml`) with your **Elastic Cloud** credentials. The script will output the correct **MySQL/Redis** configuration which you can use for the `spring.datasource` and `spring.redis` properties.

Press **`Y`** to continue the script, which will build the project and run the Spring Boot application.

---

## 3. Testing Workflow and Key Endpoints 🧪

### Swagger UI
Access the Swagger UI for interactive testing:  
👉 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

### Step 3.1: Data Initialization (⚠️ CRITICAL FIRST STEP)

You **must run the synchronization endpoint once** to index MySQL data into the remote Elasticsearch cluster.

| Action        | Method | Endpoint                | Description                                                                 |
|---------------|--------|-------------------------|-----------------------------------------------------------------------------|
| Initial Sync  | POST   | `/api/admin/sync/full` | **CRITICAL**: Executes `SyncService` to read all existing schedules from MySQL and index them in Elastic Cloud. Run this immediately. |

---

### Step 3.2: Testing Search, Lock, and Booking

These tests validate that the system correctly **calculates availability** before a booking is made and **locks the resource** during the attempt.

| Endpoint              | Test Action                          | Logic Verified                                                                 |
|-----------------------|--------------------------------------|--------------------------------------------------------------------------------|
| `/api/search/schedules` | Search for `11 → 10` (Mumbai → Pune) | **Segment Overlap**: Verifies availability against existing segmented bookings |
| `/api/v1/bookings`     | Use `cURL` to POST a booking request | **Booking Flow Integrity**: Service must acquire a Redis Distributed Lock on the seats before committing to MySQL |

---

✅ This demonstrates:
- Search queries use **Elasticsearch** for fast filtering.  
- **MySQL + Java layer** ensures **high-integrity segment validation**.  
