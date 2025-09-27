# ************************************************************
# Sequel Ace SQL dump
# Version 20050
#
# https://sequel-ace.com/
# https://github.com/Sequel-Ace/Sequel-Ace
#
# Host: 127.0.0.1 (MySQL 9.3.0)
# Database: bus_booking_db
# Generation Time: 2025-09-27 07:38:35 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE='NO_AUTO_VALUE_ON_ZERO', SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table addon
# ------------------------------------------------------------

DROP TABLE IF EXISTS `addon`;

CREATE TABLE `addon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=604 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table booking
# ------------------------------------------------------------

DROP TABLE IF EXISTS `booking`;

CREATE TABLE `booking` (
  `id` varchar(255) NOT NULL,
  `booking_time` datetime(6) NOT NULL,
  `expiration_time` datetime(6) NOT NULL,
  `final_price` decimal(38,2) NOT NULL,
  `status` enum('PENDING','CONFIRMED','CANCELLED','EXPIRED') NOT NULL,
  `user_id` bigint NOT NULL,
  `end_stop_id` bigint NOT NULL,
  `schedule_id` bigint NOT NULL,
  `start_stop_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsjwyv8sa19sdgd54f2jr185q3` (`end_stop_id`),
  KEY `FKdkador5s62nuvhdf672u65fs9` (`schedule_id`),
  KEY `FKkkw7q431sy85s9as6d75fgu3c` (`start_stop_id`),
  CONSTRAINT `FKdkador5s62nuvhdf672u65fs9` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`),
  CONSTRAINT `FKkkw7q431sy85s9as6d75fgu3c` FOREIGN KEY (`start_stop_id`) REFERENCES `stop` (`id`),
  CONSTRAINT `FKsjwyv8sa19sdgd54f2jr185q3` FOREIGN KEY (`end_stop_id`) REFERENCES `stop` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table booking_addon
# ------------------------------------------------------------

DROP TABLE IF EXISTS `booking_addon`;

CREATE TABLE `booking_addon` (
  `booking_id` varchar(255) NOT NULL,
  `addon_id` bigint NOT NULL,
  PRIMARY KEY (`booking_id`,`addon_id`),
  KEY `FK25c9l28ph84drs65iuuim0c70` (`addon_id`),
  CONSTRAINT `FK1fh6ir112y8f9v2vq8g5wwo7b` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`),
  CONSTRAINT `FK25c9l28ph84drs65iuuim0c70` FOREIGN KEY (`addon_id`) REFERENCES `addon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table booking_seat
# ------------------------------------------------------------

DROP TABLE IF EXISTS `booking_seat`;

CREATE TABLE `booking_seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `booking_id` varchar(255) NOT NULL,
  `seat_id` bigint NOT NULL,
  `segment_end_stop_id` bigint NOT NULL,
  `segment_start_stop_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3gcy7w2me25kc4qp8nobmg4q6` (`booking_id`),
  KEY `FK3y806wtfhomwvu02t1u7u2136` (`seat_id`),
  KEY `FKt41h80phqcvxfabnvm4g20mug` (`segment_end_stop_id`),
  KEY `FKma46gbnsv6pn6axa28ektr9r9` (`segment_start_stop_id`),
  CONSTRAINT `FK3gcy7w2me25kc4qp8nobmg4q6` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`id`),
  CONSTRAINT `FK3y806wtfhomwvu02t1u7u2136` FOREIGN KEY (`seat_id`) REFERENCES `seat` (`id`),
  CONSTRAINT `FKma46gbnsv6pn6axa28ektr9r9` FOREIGN KEY (`segment_start_stop_id`) REFERENCES `stop` (`id`),
  CONSTRAINT `FKt41h80phqcvxfabnvm4g20mug` FOREIGN KEY (`segment_end_stop_id`) REFERENCES `stop` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table bus
# ------------------------------------------------------------

DROP TABLE IF EXISTS `bus`;

CREATE TABLE `bus` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator` varchar(255) NOT NULL,
  `registration_number` varchar(255) NOT NULL,
  `total_seats` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_dj4x1q2ecf046w7sri9wkek02` (`registration_number`)
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table route
# ------------------------------------------------------------

DROP TABLE IF EXISTS `route`;

CREATE TABLE `route` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `distance_km` int DEFAULT NULL,
  `dest_stop_id` bigint NOT NULL,
  `source_stop_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK623yft156bxyfewgyltwgyfd4` (`dest_stop_id`),
  KEY `FKl8jp6qyoypw6jr2h8b1b1y5g3` (`source_stop_id`),
  CONSTRAINT `FK623yft156bxyfewgyltwgyfd4` FOREIGN KEY (`dest_stop_id`) REFERENCES `stop` (`id`),
  CONSTRAINT `FKl8jp6qyoypw6jr2h8b1b1y5g3` FOREIGN KEY (`source_stop_id`) REFERENCES `stop` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table schedule
# ------------------------------------------------------------

DROP TABLE IF EXISTS `schedule`;

CREATE TABLE `schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_price` decimal(10,2) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `departure_time` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `bus_id` bigint NOT NULL,
  `route_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKk6v9b839hg5skqdiuwd0nyw8x` (`bus_id`),
  KEY `FKnijrqlnbae9vvpgj6pnaqrl0q` (`route_id`),
  CONSTRAINT `FKk6v9b839hg5skqdiuwd0nyw8x` FOREIGN KEY (`bus_id`) REFERENCES `bus` (`id`),
  CONSTRAINT `FKnijrqlnbae9vvpgj6pnaqrl0q` FOREIGN KEY (`route_id`) REFERENCES `route` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=304 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table schedule_stop
# ------------------------------------------------------------

DROP TABLE IF EXISTS `schedule_stop`;

CREATE TABLE `schedule_stop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `arrival_time` datetime(6) DEFAULT NULL,
  `stop_order` int NOT NULL,
  `schedule_id` bigint NOT NULL,
  `stop_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtrm4cvjbjkh19287mlaa63l9p` (`schedule_id`),
  KEY `FKm2qo7ckeisgyjonup2piotk50` (`stop_id`),
  CONSTRAINT `FKm2qo7ckeisgyjonup2piotk50` FOREIGN KEY (`stop_id`) REFERENCES `stop` (`id`),
  CONSTRAINT `FKtrm4cvjbjkh19287mlaa63l9p` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=404 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table seat
# ------------------------------------------------------------

DROP TABLE IF EXISTS `seat`;

CREATE TABLE `seat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `multiplier` decimal(4,2) NOT NULL,
  `seat_class` enum('SEATING','AC_SEATING','SLEEPER','AC_SLEEPER','AC_SLEEPER_AND_SEATER') NOT NULL,
  `seat_number` varchar(255) NOT NULL,
  `schedule_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKppyv67e00qxortqrtlmr7gfdo` (`schedule_id`),
  CONSTRAINT `FKppyv67e00qxortqrtlmr7gfdo` FOREIGN KEY (`schedule_id`) REFERENCES `schedule` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=509 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



# Dump of table stop
# ------------------------------------------------------------

DROP TABLE IF EXISTS `stop`;

CREATE TABLE `stop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

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

