-- Database Setup Automation: Comprehensive Sample Data
-- This script populates the database with realistic Indian context data.

-- 1. CLEANUP (TRUNCATE tables to ensure a clean start)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE booking_addon;
TRUNCATE TABLE booking_seat;
TRUNCATE TABLE booking;
TRUNCATE TABLE addon;
TRUNCATE TABLE seat;
TRUNCATE TABLE schedule_stop;
TRUNCATE TABLE schedule;
TRUNCATE TABLE route;
TRUNCATE TABLE bus;
TRUNCATE TABLE stop;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. INSERT STOPS (Indian Transportation Context)
INSERT INTO stop (id, name, city) VALUES
(10, 'Pune', 'Pune'),
(11, 'Mumbai', 'Mumbai'),
(12, 'Bangalore', 'Bengaluru'),
(13, 'Chennai', 'Chennai'),
(14, 'Mysore', 'Mysore');

-- 3. INSERT ROUTES (Logical distance approximations for pricing: ~₹13/km)
-- Route (id, source_stop_id, dest_stop_id, distance_km)
INSERT INTO route (id, source_stop_id, dest_stop_id, distance_km) VALUES
(101, 11, 12, 980),   -- Mumbai to Bangalore (~980 km)
(102, 12, 13, 350),   -- Bangalore to Chennai (~350 km)
(103, 10, 14, 850);   -- Pune to Mysore (~850 km)

-- 4. INSERT BUSES (Major Bus Players)
-- Bus (id, registration_number, operator, total_seats)
INSERT INTO bus (id, registration_number, operator, total_seats) VALUES
(201, 'MH12AB0001', 'Royal Buses', 45),
(202, 'KA01CD2002', 'KSRTC', 30), -- Smaller Sleeper/AC configuration
(203, 'UP70EF3003', 'UPSRTC', 50);

-- 5. INSERT SCHEDULES (Links Bus and Route, with Base Price calculation)
-- Base Price calculation: Distance * ₹13/km
-- Schedule 101: Mumbai (11) -> Bangalore (12). Distance 980 km. Base Price = 980 * 13 = ₹12740
INSERT INTO schedule (id, bus_id, route_id, departure_time, base_price, created_at, updated_at) VALUES
(301, 201, 101, NOW() + INTERVAL 2 DAY + INTERVAL 20 HOUR, 12740.00, NOW(), NOW());
-- Schedule 102: Bangalore (12) -> Chennai (13). Distance 350 km. Base Price = 350 * 13 = ₹4550
INSERT INTO schedule (id, bus_id, route_id, departure_time, base_price, created_at, updated_at) VALUES
(302, 202, 102, NOW() + INTERVAL 1 DAY + INTERVAL 10 HOUR, 4550.00, NOW(), NOW());
-- Schedule 103: Pune (10) -> Mysore (14). Distance 850 km. Base Price = 850 * 13 = ₹11050
INSERT INTO schedule (id, bus_id, route_id, departure_time, base_price, created_at, updated_at) VALUES
(303, 203, 103, NOW() + INTERVAL 3 DAY + INTERVAL 15 HOUR, 11050.00, NOW(), NOW());

-- 6. INSERT SCHEDULE STOPS (Multi-hop stops for Schedule 301: Mumbai -> Pune -> Bangalore)
-- ScheduleStop (id, schedule_id, stop_id, stop_order, arrival_time)
INSERT INTO schedule_stop (id, schedule_id, stop_id, stop_order, arrival_time) VALUES
(401, 301, 11, 1, NOW() + INTERVAL 20 HOUR),      -- Mumbai (Source)
(402, 301, 10, 2, NOW() + INTERVAL 2 DAY),         -- Pune (Intermediate Stop)
(403, 301, 12, 3, NOW() + INTERVAL 2 DAY + INTERVAL 10 HOUR); -- Bangalore (Destination)

-- 7. INSERT SEATS (Multi-class Seating for Schedule 301)
-- Seat (id, schedule_id, seat_number, seat_class, multiplier)
-- AC_SLEEPER (4.0x)
INSERT INTO seat (id, schedule_id, seat_number, seat_class, multiplier) VALUES
(501, 301, 'A1', 'AC_SLEEPER', 4.00),
(502, 301, 'A2', 'AC_SLEEPER', 4.00);

-- AC_SEATING (1.5x)
INSERT INTO seat (id, schedule_id, seat_number, seat_class, multiplier) VALUES
(503, 301, 'B1', 'AC_SEATING', 1.50),
(504, 301, 'B2', 'AC_SEATING', 1.50);

-- SLEEPER (2.5x)
INSERT INTO seat (id, schedule_id, seat_number, seat_class, multiplier) VALUES
(505, 301, 'C1', 'SLEEPER', 2.50),
(506, 301, 'C2', 'SLEEPER', 2.50);

-- SEATING (1.0x) - Assuming remaining seats are standard seating
INSERT INTO seat (id, schedule_id, seat_number, seat_class, multiplier) VALUES
(507, 301, 'D1', 'SEATING', 1.00),
(508, 301, 'D2', 'SEATING', 1.00);

-- 8. INSERT ADDONS (Decorator Pattern components)
-- Addon (id, name, description, price)
INSERT INTO addon (id, name, description, price) VALUES
(601, 'EXTRA_LUGGAGE', 'Allows 1 extra bag up to 15 kg.', 150.00),
(602, 'MEAL_COMBO', 'Veg/Non-veg meal combo.', 250.00),
(603, 'PRIORITY_BOARDING', 'Early access to the bus and overhead space.', 100.00);
