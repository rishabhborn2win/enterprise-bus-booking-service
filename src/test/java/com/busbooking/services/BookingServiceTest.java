package com.busbooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.busbooking.domains.dto.BookingRequest;
import com.busbooking.domains.dto.BookingResponse;
import com.busbooking.entities.*;
import com.busbooking.enums.BookingStatus;
import com.busbooking.exceptions.ConcurrencyException;
import com.busbooking.exceptions.PaymentFailureException;
import com.busbooking.exceptions.ResourceNotFoundException;
import com.busbooking.exceptions.SeatUnavailableException;
import com.busbooking.repositories.*;
import com.busbooking.strategies.DynamicPriceStrategy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private StopRepository stopRepository;
    @Mock private AddonRepository addonRepository;
    @Mock private BookingSeatRepository bookingSeatRepository;
    @Mock private RedissonClient redissonClient;
    @Mock private DynamicPriceStrategy dynamicPricingStrategy;
    @Mock private RLock rLock;

    @InjectMocks private BookingService bookingService;

    private Schedule schedule;
    private Stop startStop;
    private Stop endStop;
    private Seat seat;
    private BookingRequest bookingRequest;

    @BeforeEach
    void setUp() {
        // Common test data setup
        schedule = new Schedule();
        schedule.setId(1L);

        startStop = new Stop();
        startStop.setId(101L);

        endStop = new Stop();
        endStop.setId(102L);

        seat = new Seat();
        seat.setId(1L);
        seat.setSeatNumber("A1");
        seat.setSchedule(schedule);

        bookingRequest = new BookingRequest();
        bookingRequest.setScheduleId(1L);
        bookingRequest.setStartStopId(101L);
        bookingRequest.setEndStopId(102L);
        bookingRequest.setUserId(123L);
        bookingRequest.setSeatNumbers(Collections.singletonList("A1"));
    }

    private void mockValidScheduleAndStops() {
        ScheduleStop scheduleStartStop = new ScheduleStop();
        scheduleStartStop.setStop(startStop);
        scheduleStartStop.setStopOrder(1);

        ScheduleStop scheduleEndStop = new ScheduleStop();
        scheduleEndStop.setStop(endStop);
        scheduleEndStop.setStopOrder(2);

        schedule.setStops(List.of(scheduleStartStop, scheduleEndStop));

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(stopRepository.findById(101L)).thenReturn(Optional.of(startStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(endStop));
        when(seatRepository.findByScheduleId(1L)).thenReturn(Collections.singletonList(seat));
    }

    @Test
    void reserveSeats_Success() throws InterruptedException {
        mockValidScheduleAndStops();
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(dynamicPricingStrategy.calculatePrice(any(), any()))
                .thenReturn(BigDecimal.valueOf(100));
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(
                        invocation -> {
                            Booking b = invocation.getArgument(0);
                            b.setId("booking-123"); // Simulate saving and getting an ID
                            return b;
                        });

        BookingResponse response = bookingService.reserveSeats(bookingRequest);

        assertNotNull(response);
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals("booking-123", response.getBookingId());
        verify(redissonClient, times(1)).getLock(anyString());
        verify(rLock, times(1)).tryLock(anyLong(), anyLong(), any());
        verify(bookingRepository, times(1)).saveAndFlush(any(Booking.class));
    }

    @Test
    void reserveSeats_FailsWhenScheduleNotFound() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class, () -> bookingService.reserveSeats(bookingRequest));
    }

    @Test
    void reserveSeats_FailsWhenStopOrderIsInvalid() {
        // Make end stop come before start stop
        ScheduleStop scheduleStartStop = new ScheduleStop();
        scheduleStartStop.setStop(startStop);
        scheduleStartStop.setStopOrder(2);

        ScheduleStop scheduleEndStop = new ScheduleStop();
        scheduleEndStop.setStop(endStop);
        scheduleEndStop.setStopOrder(1);

        schedule.setStops(List.of(scheduleStartStop, scheduleEndStop));

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(stopRepository.findById(101L)).thenReturn(Optional.of(startStop));
        when(stopRepository.findById(102L)).thenReturn(Optional.of(endStop));

        assertThrows(
                IllegalArgumentException.class, () -> bookingService.reserveSeats(bookingRequest));
    }

    @Test
    void reserveSeats_FailsWhenLockCannotBeAcquired() throws InterruptedException {
        mockValidScheduleAndStops();
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        assertThrows(ConcurrencyException.class, () -> bookingService.reserveSeats(bookingRequest));
        verify(rLock, times(1)).unlock(); // Ensure lock is released on failure
    }

    @Test
    void reserveSeats_FailsWhenSeatIsAlreadyReserved() {
        mockValidScheduleAndStops();
        // Mock the optimized query to return an overlapping segment
        Object[] bookedSegment = new Object[] {1L, 1, 3}; // seatId, startOrder, endOrder
        when(bookingSeatRepository.findConfirmedBookedSeatSegmentsWithOrders(1L))
                .thenReturn(Collections.singletonList(bookedSegment));

        assertThrows(
                SeatUnavailableException.class, () -> bookingService.reserveSeats(bookingRequest));
    }

    @Test
    void confirmBooking_Success() {
        Booking booking = new Booking();
        booking.setId("booking-123");
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        booking.setSchedule(schedule);
        booking.setStartStop(startStop);
        booking.setEndStop(endStop);
        BookingSeat e1 = new BookingSeat();
        e1.setBooking(booking);
        Seat seat1 = new Seat();
        seat1.setId(1L);
        e1.setSeat(seat1);
        booking.setReservedSeats(Set.of(e1)); // Initialize to avoid NPE

        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.isLocked()).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        BookingResponse response = bookingService.confirmBooking("booking-123", "payment-abc");

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(rLock, times(1)).unlock();
    }

    @Test
    void confirmBooking_FailsWhenBookingNotFound() {
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> bookingService.confirmBooking("booking-123", "payment-abc"));
    }

    @Test
    void confirmBooking_FailsWhenBookingIsNotPending() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThrows(
                PaymentFailureException.class,
                () -> bookingService.confirmBooking("booking-123", "payment-abc"));
    }

    @Test
    void confirmBooking_FailsWhenBookingIsExpired() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpirationTime(LocalDateTime.now().minusMinutes(1));
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThrows(
                PaymentFailureException.class,
                () -> bookingService.confirmBooking("booking-123", "payment-abc"));
    }

    @Test
    void cancelBooking_Success() {
        Booking booking = new Booking();
        booking.setId("booking-123");
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSchedule(schedule);
        booking.setStartStop(startStop);
        booking.setEndStop(endStop);
        booking.setReservedSeats(Set.of());

        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking("booking-123");

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void cancelBooking_FailsWhenNotConfirmed() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById("booking-123")).thenReturn(Optional.of(booking));

        assertThrows(
                PaymentFailureException.class, () -> bookingService.cancelBooking("booking-123"));
    }
}
