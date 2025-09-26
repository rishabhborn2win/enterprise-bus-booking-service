package com.busbooking.services;

import com.busbooking.components.BasicBooking;
import com.busbooking.components.BookingComponent;
import com.busbooking.components.ExtraLuggageDecorator;
import com.busbooking.components.MealDecorator;
import com.busbooking.components.PriorityBoardingDecorator;
import com.busbooking.domains.dto.BookingRequest;
import com.busbooking.domains.dto.BookingResponse;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.exceptions.ConcurrencyException;
import com.busbooking.exceptions.PaymentFailureException;
import com.busbooking.exceptions.ResourceNotFoundException;
import com.busbooking.exceptions.SeatUnavailableException;
import com.busbooking.repositories.BookingRepository;
import com.busbooking.repositories.BookingSeatRepository;
import com.busbooking.repositories.SeatRepository;
import com.busbooking.repositories.ScheduleRepository;
import com.busbooking.repositories.StopRepository;
import com.busbooking.repositories.AddonRepository;
import com.busbooking.entities.Booking;
import com.busbooking.entities.Seat;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.Stop;
import com.busbooking.entities.Addon;
import com.busbooking.entities.BookingSeat;
import com.busbooking.enums.BookingStatus;
import com.busbooking.strategies.DynamicPriceStrategy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service Layer Pattern: Encapsulates all core business logic for the booking lifecycle.
 * Focuses on Concurrency Control and Multi-hop inventory management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ScheduleRepository scheduleRepository;
    private final StopRepository stopRepository;
    private final AddonRepository addonRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final RedissonClient redissonClient;
    private final DynamicPriceStrategy dynamicPricingStrategy; // Injected from Controller @Bean
    @PersistenceContext private EntityManager entityManager;


    /**
     * API: Reserves seats, creates a PENDING booking, and applies a distributed lock.
     * CRITICAL: Prevents overbooking using Redis locks with a timeout.
     * @param request The BookingRequest DTO.
     * @return BookingResponse with PENDING status and expiration time.
     */
    @Transactional
    public BookingResponse reserveSeats(BookingRequest request) {
        log.info("Attempting to reserve seats for Schedule ID: {}", request.getScheduleId());

        // 1. Data Validation & Fetching
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", request.getScheduleId()));
        Stop startStop = stopRepository.findById(request.getStartStopId())
                .orElseThrow(() -> new ResourceNotFoundException("Stop", "id", request.getStartStopId()));
        Stop endStop = stopRepository.findById(request.getEndStopId())
                .orElseThrow(() -> new ResourceNotFoundException("Stop", "id", request.getEndStopId()));
        List<Seat> allSeats = seatRepository.findByScheduleId(request.getScheduleId());

        if (startStop.getId().equals(endStop.getId())) {
            throw new IllegalArgumentException("Start and End stops cannot be the same.");
        }

        // Validate that startStop and endStop are valid stops in the schedule and in correct order
        List<ScheduleStop> scheduleStops = schedule.getStops();
        if (scheduleStops == null || scheduleStops.isEmpty()) {
            throw new IllegalArgumentException("No stops defined for this schedule.");
        }
        Integer startOrder = null;
        Integer endOrder = null;
        for (ScheduleStop ss : scheduleStops) {
            if (ss.getStop().getId().equals(startStop.getId())) {
                startOrder = ss.getStopOrder();
            }
            if (ss.getStop().getId().equals(endStop.getId())) {
                endOrder = ss.getStopOrder();
            }
        }
        if (startOrder == null || endOrder == null) {
            throw new IllegalArgumentException("Start or End stop is not part of the route for this schedule.");
        }
        if (startOrder >= endOrder) {
            throw new IllegalArgumentException("Start stop must come before end stop in the route order.");
        }

        // 2. Seat Selection & Locking Preparation
        List<Seat> seatsToReserve = selectSeats(request, allSeats);

        // Define the segment for locking (CRITICAL for Multi-hop)
        String segmentKey = String.format("%s-%s", startStop.getId(), endStop.getId());
        List<RLock> locks = new ArrayList<>();

        try {
            // Acquire locks for each selected seat on the specific segment
            for (Seat seat : seatsToReserve) {
                // Lock Key: lock:scheduleId:10001:seat:A5:segment:17-18
                String lockKey = String.format("lock:schedule:%d:seat:%d:segment:%s",
                        schedule.getId(), seat.getId(), segmentKey);
                RLock lock = redissonClient.getLock(lockKey);

                // Attempt to acquire lock for 10 seconds, and hold it for the booking duration (10 mins)
                // This ensures Zero Overbooking using an atomic, distributed mechanism.
                boolean isLocked = lock.tryLock(0, 10, TimeUnit.MINUTES);
                if (!isLocked) {
                    throw new ConcurrencyException(String.format("Seat %s is currently reserved or locked by another user.", seat.getSeatNumber()));
                }
                locks.add(lock);
            }
            log.info("Successfully acquired distributed locks for {} seats.", seatsToReserve.size());

            // 3. Create PENDING Booking (MySQL Transaction)
            Booking booking = new Booking();
            booking.setUserId(request.getUserId());
            booking.setSchedule(schedule);
            booking.setStartStop(startStop);
            booking.setEndStop(endStop);
            booking.setStatus(BookingStatus.PENDING);
            booking.setBookingTime(LocalDateTime.now());
            // 10-minute hold for payment processing
            booking.setExpirationTime(LocalDateTime.now().plusMinutes(10));

            // 4. Dynamic Pricing & Addons (Decorator/Strategy Patterns)
            BigDecimal basePrice = dynamicPricingStrategy.calculatePrice(booking, seatsToReserve);

            // Fetch Addons
            Set<Addon> addons = request.getAddonIds() != null
                    ? addonRepository.findAllById(request.getAddonIds()).stream().collect(Collectors.toSet())
                    : new HashSet<>();
            booking.setAddons(addons);

            // Apply Decorator Pattern for Addon pricing
            BookingComponent priceComponent = new BasicBooking(basePrice);
            for (Addon addon : addons) {
                if (addon.getName().contains("LUGGAGE")) {
                    priceComponent = new ExtraLuggageDecorator(priceComponent);
                } else if (addon.getName().contains("MEAL")) {
                    priceComponent = new MealDecorator(priceComponent);
                } else if (addon.getName().contains("PRIORITY")) {
                    priceComponent = new PriorityBoardingDecorator(priceComponent);
                }
            }
            booking.setFinalPrice(priceComponent.calculatePrice());

            // 5. Save Booking and Reserved Seats
            Booking savedBooking = bookingRepository.save(booking);

            seatsToReserve.forEach(seat -> {
                BookingSeat bs = new BookingSeat();
                bs.setBooking(savedBooking);
                bs.setSeat(seat);
                bs.setSegmentStartStop(startStop);
                bs.setSegmentEndStop(endStop);
                savedBooking.getReservedSeats().add(bs);
            });

            // Re-save to persist BookingSeat relations
            bookingRepository.saveAndFlush(savedBooking);

            log.info("Booking ID {} successfully created with PENDING status. Price: {}", savedBooking.getId(), savedBooking.getFinalPrice());
            return mapToResponse(savedBooking);

        } catch (InterruptedException e) {
            log.error("Reservation failed due to interrupted thread (lock failure): {}", e.getMessage());
            // Release all acquired locks on failure
            locks.forEach(RLock::unlock);
            // Throw a ConcurrencyException to ensure transaction rollback
            throw new ConcurrencyException("Reservation failed due to system concurrency issue.");
        } catch (RuntimeException e) {
            // Ensure locks are released even on other runtime failures (e.g., DB error, SeatUnavailableException)
            locks.forEach(RLock::unlock);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Helper to determine seats for reservation, falling back to auto-assignment.
     */
    private List<Seat> selectSeats(BookingRequest request, List<Seat> allSeats) {
        if (request.getSeatNumbers() == null || request.getSeatNumbers().isEmpty()) {
            log.info("No seats manually selected. Performing intelligent auto-assignment.");
            // INTELLIGENT AUTO-ASSIGNMENT FALLBACK: Simple logic - assign first available seats
            return allSeats.stream()
                    .filter(seat -> !isSeatCurrentlyReserved(seat.getId(), seat.getSchedule().getId(), request.getStartStopId(), request.getEndStopId()))
                    .limit(1) // Assign 1 seat for simplicity
                    .collect(Collectors.toList());
        } else {
            // MANUAL SELECTION
            return request.getSeatNumbers().stream()
                    .map(seatNum -> allSeats.stream()
                            .filter(s -> s.getSeatNumber().equals(seatNum))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Seat", "number", seatNum))
                    )
                    .peek(seat -> {
                        if (isSeatCurrentlyReserved(seat.getId(), seat.getSchedule().getId(), request.getStartStopId(), request.getEndStopId())) {
                            throw new SeatUnavailableException(String.format("Seat %s is already reserved/locked.", seat.getSeatNumber()));
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Checks if the seat is reserved for any overlapping segment in the same schedule.
     * Prevents booking if the requested segment overlaps with any confirmed booking for the seat.
     * Overlap is defined as any intersection between [reqStartOrder, reqEndOrder) and [bookedStartOrder, bookedEndOrder).
     */
    private boolean isSeatCurrentlyReserved(Long seatId, Long scheduleId, Long startStopId, Long endStopId) {
        // further optimisation filter the booking seats for expired/cancelled bookings
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", scheduleId));
        List<ScheduleStop> stops = schedule.getStops();
        Map<Long, Integer> stopOrderMap = new HashMap<>();
        for (ScheduleStop ss : stops) {
            stopOrderMap.put(ss.getStop().getId(), ss.getStopOrder());
        }
        Integer reqStartOrder = stopOrderMap.get(startStopId);
        Integer reqEndOrder = stopOrderMap.get(endStopId);
        if (reqStartOrder == null || reqEndOrder == null) {
            throw new IllegalArgumentException("Start or End stop is not part of the route for this schedule.");
        }
        // For each confirmed booking, check for overlap
        return bookingRepository.findAll().stream()
                .filter(b -> b.getSchedule().getId().equals(scheduleId))
                .flatMap(b -> b.getReservedSeats().stream())
                .filter(bs -> bs.getSeat().getId().equals(seatId) && (bs.getBooking().getStatus() == BookingStatus.CONFIRMED || bs.getBooking().getStatus() == BookingStatus.PENDING))
                .anyMatch(bs -> {
                    Integer bookedStartOrder = stopOrderMap.get(bs.getSegmentStartStop().getId());
                    Integer bookedEndOrder = stopOrderMap.get(bs.getSegmentEndStop().getId());
                    if (bookedStartOrder == null || bookedEndOrder == null) return false;
                    // Overlap: (reqStart < bookedEnd) && (bookedStart < reqEnd)
                    return reqStartOrder < bookedEndOrder && bookedStartOrder < reqEndOrder;
                });
    }


    /**
     * API: Confirms a PENDING booking after successful payment processing.
     * @param bookingId The ID of the PENDING booking.
     * @param paymentTransactionId Mock payment reference.
     * @return BookingResponse with CONFIRMED status.
     */
    @Transactional
    public BookingResponse confirmBooking(String bookingId, String paymentTransactionId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new PaymentFailureException("Booking is not in PENDING state or has already expired/cancelled. Current status: " + booking.getStatus());
        }
        
        // CRITICAL: Check if the booking has already expired just before confirming
        if (booking.getExpirationTime().isBefore(LocalDateTime.now())) {
            // Trigger manual expiration cleanup if missed by the scheduler
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new PaymentFailureException("Booking reservation timed out and expired before payment confirmation.");
        }

        // 1. Simulate successful payment validation (simple check)
        if (paymentTransactionId == null || paymentTransactionId.isBlank()) {
             throw new PaymentFailureException("Payment failed or transaction ID is missing.");
        }

        // 2. Update Status
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        // 3. Notification (Conceptual: Send event to Notification Service)
        log.info("Booking ID {} confirmed. Sending confirmation event to Notification Service.", bookingId);

        // 4. Release Locks (It is best practice to release Redisson locks early, though they will expire naturally)
        releaseLocksForBooking(booking);

        return mapToResponse(savedBooking);
    }

    /**
     * API: Cancels a CONFIRMED booking and releases seats.
     * @param bookingId The ID of the booking to cancel.
     * @return BookingResponse with CANCELLED status.
     */
    @Transactional
    public BookingResponse cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            log.warn("Attempt to cancel an already non-active booking: {}", bookingId);
            return mapToResponse(booking);
        }
        
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new PaymentFailureException("Only CONFIRMED bookings can be cancelled by the user. Current status: " + booking.getStatus());
        }

        // 1. Update Status
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        // 2. Seat Release (Inventory restoration)
        // In a real system, a message would be sent to the Search/Inventory service to update availability immediately.
        log.info("Booking ID {} cancelled. Seats released back to inventory.", bookingId);

        return mapToResponse(savedBooking);
    }

    /**
     * Releases the distributed Redisson locks associated with a booking.
     * Called upon successful confirmation to free up the lock key early.
     */
    private void releaseLocksForBooking(Booking booking) {
        String segmentKey = String.format("%s-%s", booking.getStartStop().getId(), booking.getEndStop().getId());
        booking.getReservedSeats().forEach(bookingSeat -> {
            String lockKey = String.format("lock:schedule:%d:seat:%d:segment:%s",
                    booking.getSchedule().getId(), bookingSeat.getSeat().getId(), segmentKey);
            RLock lock = redissonClient.getLock(lockKey);

            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released for seat {}", bookingSeat.getSeat().getSeatNumber());
            } else {
                 log.debug("Lock for seat {} was not held by current thread or was already released.", bookingSeat.getSeat().getSeatNumber());
            }
        });
    }

    /**
     * Maps the JPA entity to the simplified DTO for API responses.
     */
    private BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setBookingId(booking.getId());
        response.setScheduleId(booking.getSchedule().getId());
        response.setUserTicketRef(booking.getId().substring(0, 8)); // Simple ref
        response.setStatus(booking.getStatus());
        response.setFinalPrice(booking.getFinalPrice());
        response.setBookingTime(booking.getBookingTime());
        response.setExpirationTime(booking.getExpirationTime());

        response.setReservedSeats(booking.getReservedSeats().stream()
                .map(bs -> bs.getSeat().getSeatNumber())
                .collect(Collectors.toList()));

        response.setAddons(booking.getAddons().stream()
                .map(Addon::getName)
                .collect(Collectors.toList()));

        return response;
    }
}
