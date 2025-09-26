# Bus Booking Application

## Overview
This is a Spring Boot-based RESTful service for bus seat booking, supporting multi-hop inventory management, concurrency control, and dynamic pricing. The application exposes endpoints for reserving, confirming, and cancelling bookings, and provides interactive API documentation via Swagger UI.

## Features
- Reserve seats with distributed locking to prevent overbooking
- Confirm bookings after payment
- Cancel bookings and release seats
- Multi-hop segment support (source/destination validation)
- Dynamic and addon-based pricing
- OpenAPI 3.0 (Swagger) documentation

## Prerequisites
- Java 17 or higher
- Maven 3.8+
- Redis (for distributed locking)
- Local database (H2, MySQL, or PostgreSQL)

## Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/your-username/BusBookingApplication.git
cd BusBookingApplication
```

### 2. Configure Database & Redis
- Update `src/main/resources/application.yaml` with your database and Redis connection details.
- Example for H2 (default):
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:mem:busbooking;DB_CLOSE_DELAY=-1
      username: sa
      password:
    redis:
      host: localhost
      port: 6379
  ```

### 3. Build the Project
```bash
mvn clean install
```

### 4. Run the Application
```bash
mvn spring-boot:run
```
Or, run the generated JAR:
```bash
java -jar target/BusBookingServiceApplication.jar
```

### 5. Access Swagger API Documentation
Once the application is running, open your browser and navigate to:
```
http://localhost:8080/swagger-ui/index.html
```
This interactive UI lets you explore and test all API endpoints, view request/response models, and see error codes.

## API Endpoints
- `POST /api/v1/bookings` — Reserve seats (PENDING booking)
- `POST /api/v1/bookings/{id}/confirm` — Confirm booking after payment
- `DELETE /api/v1/bookings/{id}/cancel` — Cancel booking and release seats

## Example Usage
You can use Swagger UI to:
- Try out booking requests
- Confirm bookings with payment transaction IDs
- Cancel bookings

## Troubleshooting
- Ensure Redis and your database are running locally
- Check `application.yaml` for correct connection settings
- For port conflicts, change the `server.port` property in `application.yaml`

## Contributing
Feel free to fork and submit pull requests. For major changes, open an issue first to discuss your ideas.

## License
MIT

---
For more details, see the Swagger UI or contact the project maintainer.

