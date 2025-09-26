-- Drop tables to ensure clean setup (Development only)
DROP TABLE IF EXISTS booking_addon;
DROP TABLE IF EXISTS booking_seat;
DROP TABLE IF EXISTS addon;
DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS seat;
DROP TABLE IF EXISTS schedule_stop;
DROP TABLE IF EXISTS schedule;
DROP TABLE IF EXISTS route;
DROP TABLE IF EXISTS stop;
DROP TABLE IF EXISTS bus;

-- 1. BUS Table (Major Bus Players)
CREATE TABLE bus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    registration_number VARCHAR(15) NOT NULL UNIQUE,
    operator VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL,
    bus_type VARCHAR(50) NOT NULL -- AC/Non-AC, Seater/Sleeper
);

-- 2. STOP Table (Indian Cities/Stops)
CREATE TABLE stop (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    city VARCHAR(50) NOT NULL
);

-- 3. ROUTE Table
CREATE TABLE route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_stop_id BIGINT NOT NULL,
    dest_stop_id BIGINT NOT NULL,
    distance_km INT NOT NULL,
    FOREIGN KEY (source_stop_id) REFERENCES stop(id),
    FOREIGN KEY (dest_stop_id) REFERENCES stop(id)
);

-- 4. SCHEDULE Table
CREATE TABLE schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bus_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    departure_time DATETIME NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL, -- Base fare for the full route
    FOREIGN KEY (bus_id) REFERENCES bus(id),
    FOREIGN KEY (route_id) REFERENCES route(id)
);

-- 5. SCHEDULE_STOP Table (For Multi-hop support)
CREATE TABLE schedule_stop (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    stop_id BIGINT NOT NULL,
    stop_order INT NOT NULL,
    arrival_time DATETIME NOT NULL,
    FOREIGN KEY (schedule_id) REFERENCES schedule(id),
    FOREIGN KEY (stop_id) REFERENCES stop(id),
    UNIQUE KEY uk_schedule_order (schedule_id, stop_order)
);

-- 6. SEAT Table (Multi-class seating)
CREATE TABLE seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    seat_number VARCHAR(5) NOT NULL,
    seat_class VARCHAR(50) NOT NULL,
    price_multiplier DECIMAL(5, 2) NOT NULL, -- 1.0x, 1.5x, 2.5x, 4.0x
    FOREIGN KEY (schedule_id) REFERENCES schedule(id)
);

-- 7. BOOKING Table
CREATE TABLE booking (
    id VARCHAR(36) PRIMARY KEY, -- Using UUID for unique booking reference
    user_id BIGINT NOT NULL, -- Mock user ID for this service
    schedule_id BIGINT NOT NULL,
    start_stop_id BIGINT NOT NULL,
    end_stop_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- PENDING, CONFIRMED, CANCELLED, EXPIRED
    final_price DECIMAL(10, 2) NOT NULL,
    booking_time DATETIME NOT NULL,
    expiration_time DATETIME NOT NULL,
    FOREIGN KEY (schedule_id) REFERENCES schedule(id),
    FOREIGN KEY (start_stop_id) REFERENCES stop(id),
    FOREIGN KEY (end_stop_id) REFERENCES stop(id)
);

-- 8. BOOKING_SEAT Table (Links seats to a specific segment of the booking)
CREATE TABLE booking_seat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id VARCHAR(36) NOT NULL,
    seat_id BIGINT NOT NULL,
    segment_start_stop_id BIGINT NOT NULL,
    segment_end_stop_id BIGINT NOT NULL,
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (seat_id) REFERENCES seat(id),
    FOREIGN KEY (segment_start_stop_id) REFERENCES stop(id),
    FOREIGN KEY (segment_end_stop_id) REFERENCES stop(id),
    UNIQUE KEY uk_seat_segment (seat_id, segment_start_stop_id, segment_end_stop_id)
);

-- 9. ADDON Table (Decorator Component)
CREATE TABLE addon (
    id INT PRIMARY KEY, -- Simple IDs for fixed addons
    name VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL
);

-- 10. BOOKING_ADDON Table (Decorator link)
CREATE TABLE booking_addon (
    booking_id VARCHAR(36) NOT NULL,
    addon_id INT NOT NULL,
    PRIMARY KEY (booking_id, addon_id),
    FOREIGN KEY (booking_id) REFERENCES booking(id),
    FOREIGN KEY (addon_id) REFERENCES addon(id)
);
