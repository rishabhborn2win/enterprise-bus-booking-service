package com.busbooking.services;

import com.busbooking.domains.dto.SeatAvailabilityResponse;
import com.busbooking.entities.Seat;
import com.busbooking.enums.SeatClass;
import com.busbooking.repositories.BookingSeatRepository;
import com.busbooking.repositories.ScheduleStopRepository;
import com.busbooking.repositories.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatAvailabilityServiceTest {

    @Mock
    private ScheduleStopRepository scheduleStopRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatAvailabilityService seatAvailabilityService;

    // Schedule and Stop IDs (matching the test_data.sql context)
    private static final Long SCHEDULE_ID = 301L;
    private static final Long MUMBAI_ID = 11L; // Order 1
    private static final Long PUNE_ID = 10L;   // Order 2
    private static final Long BANGALORE_ID = 12L; // Order 3

    private List<Seat> allSeats;
    private List<Object[]> bookedSegments;

    @BeforeEach
    void setUp() {
        // Mock All Seats for Schedule 301
        allSeats = List.of(
            // Seat ID 501: Blocked for full trip
            Seat.builder().id(501L).seatNumber("A1").seatClass(SeatClass.AC_SLEEPER).multiplier(BigDecimal.valueOf(4.0)).build(),
            // Seat ID 502: Blocked for segment 1-2
            Seat.builder().id(502L).seatNumber("A2").seatClass(SeatClass.AC_SLEEPER).multiplier(BigDecimal.valueOf(4.0)).build(),
            // Seat ID 503: Blocked for segment 2-3
            Seat.builder().id(503L).seatNumber("B1").seatClass(SeatClass.AC_SEATING).multiplier(BigDecimal.valueOf(1.5)).build(),
            // Seat ID 504: Available seat
            Seat.builder().id(504L).seatNumber("B2").seatClass(SeatClass.AC_SEATING).multiplier(BigDecimal.valueOf(1.5)).build()
        );
        
        // Mock Booked Segments Data (from BookingSeatRepository query result)
        bookedSegments = List.of(
            // Seat A1 (ID 501) booked for 1 -> 3 (Mumbai to Bangalore)
            new Object[]{501L, 1, 3},
            // Seat A2 (ID 502) booked for 1 -> 2 (Mumbai to Pune)
            new Object[]{502L, 1, 2},
            // Seat B1 (ID 503) booked for 2 -> 3 (Pune to Bangalore)
            new Object[]{503L, 2, 3}
        );
    }
    
    // --- Test Cases for Segment Overlap Logic ---

    @Test
    void getSegmentAvailability_FullTripSearch_ShouldBlockSeatsForEntireRoute() {
        // Arrange: Search MUMBAI (1) to BANGALORE (3)
        // Standard mock setup for all scenarios:
        when(seatRepository.findByScheduleId(SCHEDULE_ID)).thenReturn(allSeats);
        when(bookingSeatRepository.findConfirmedBookedSeatSegmentsWithOrders(SCHEDULE_ID)).thenReturn(bookedSegments);
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, MUMBAI_ID)).thenReturn(Optional.of(1));
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, BANGALORE_ID)).thenReturn(Optional.of(3));

        // Act
        SeatAvailabilityResponse response = seatAvailabilityService.getSegmentAvailability(SCHEDULE_ID, MUMBAI_ID, BANGALORE_ID);
        
        // Assert
        // Query Segment Order: (1, 3)
        // Seat A1 (1, 3) overlaps (1 < 3 && 3 > 1) -> UNAVAILABLE
        // Seat A2 (1, 2) overlaps (1 < 3 && 2 > 1) -> UNAVAILABLE
        // Seat B1 (2, 3) overlaps (2 < 3 && 3 > 1) -> UNAVAILABLE
        assertEquals(4, response.getTotalSeats());
        assertEquals(3, response.getBookedSeats().size(), "Should block seats A1, A2, and B1.");
        assertEquals(1, response.getAvailableSeats().size(), "Only seat B2 should be available.");
        assertTrue(response.getAvailableSeats().contains("B2"));
    }

    @Test
    void getSegmentAvailability_FirstSegmentSearch_ShouldOnlyBlockSegment1Seats() {
        // Arrange: Search MUMBAI (1) to PUNE (2)
        // Standard mock setup for all scenarios:
        when(seatRepository.findByScheduleId(SCHEDULE_ID)).thenReturn(allSeats);
        when(bookingSeatRepository.findConfirmedBookedSeatSegmentsWithOrders(SCHEDULE_ID)).thenReturn(bookedSegments);
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, MUMBAI_ID)).thenReturn(Optional.of(1));
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, PUNE_ID)).thenReturn(Optional.of(2));

        // Act
        SeatAvailabilityResponse response = seatAvailabilityService.getSegmentAvailability(SCHEDULE_ID, MUMBAI_ID, PUNE_ID);

        // Assert
        // Query Segment Order: (1, 2)
        // Seat A1 (1, 3): (1 < 2 && 3 > 1) -> UNAVAILABLE
        // Seat A2 (1, 2): (1 < 2 && 2 > 1) -> UNAVAILABLE
        // Seat B1 (2, 3): (2 < 2 && 3 > 1) -> AVAILABLE (2 < 2 is FALSE)
        assertEquals(4, response.getTotalSeats());
        assertEquals(2, response.getBookedSeats().size(), "Should block only A1 and A2.");
        assertTrue(response.getBookedSeats().containsAll(List.of("A1", "A2")));
        assertTrue(response.getAvailableSeats().containsAll(List.of("B1", "B2")));
    }

    @Test
    void getSegmentAvailability_SecondSegmentSearch_ShouldOnlyBlockSegment2Seats() {
        // Arrange: Search PUNE (2) to BANGALORE (3)
        // Standard mock setup for all scenarios:
        when(seatRepository.findByScheduleId(SCHEDULE_ID)).thenReturn(allSeats);
        when(bookingSeatRepository.findConfirmedBookedSeatSegmentsWithOrders(SCHEDULE_ID)).thenReturn(bookedSegments);
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, PUNE_ID)).thenReturn(Optional.of(2));
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, BANGALORE_ID)).thenReturn(Optional.of(3));

        // Act
        SeatAvailabilityResponse response = seatAvailabilityService.getSegmentAvailability(SCHEDULE_ID, PUNE_ID, BANGALORE_ID);

        // Assert
        // Query Segment Order: (2, 3)
        // Seat A1 (1, 3): (1 < 3 && 3 > 2) -> UNAVAILABLE
        // Seat A2 (1, 2): (1 < 3 && 2 > 2) -> AVAILABLE (2 > 2 is FALSE)
        // Seat B1 (2, 3): (2 < 3 && 3 > 2) -> UNAVAILABLE
        assertEquals(4, response.getTotalSeats());
        assertEquals(2, response.getBookedSeats().size(), "Should block A1 and B1.");
        assertTrue(response.getBookedSeats().containsAll(List.of("A1", "B1")));
        assertTrue(response.getAvailableSeats().containsAll(List.of("A2", "B2")));
    }
    
    // --- Test Cases for Invalid Input / Data Safety ---
    
    @Test
    void getSegmentAvailability_InvalidStopOrder_ThrowsException() {
        // Arrange: Search BANGALORE (3) to MUMBAI (1)
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, BANGALORE_ID)).thenReturn(Optional.of(3));
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, MUMBAI_ID)).thenReturn(Optional.of(1));

        // Act & Assert: queryStartOrder (3) >= queryEndOrder (1) should fail validation
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> seatAvailabilityService.getSegmentAvailability(SCHEDULE_ID, BANGALORE_ID, MUMBAI_ID));
        
        assertEquals("Invalid segment: Source must precede destination.", exception.getMessage());
    }

    @Test
    void getSegmentAvailability_NonexistentStop_ThrowsException() {
        // Arrange: Use a stop ID that doesn't exist on the schedule (e.g., 99)
        Long NON_EXISTENT_STOP = 99L;
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, MUMBAI_ID)).thenReturn(Optional.of(1));
        when(scheduleStopRepository.findStopOrderByScheduleIdAndStopId(SCHEDULE_ID, NON_EXISTENT_STOP)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
            () -> seatAvailabilityService.getSegmentAvailability(SCHEDULE_ID, MUMBAI_ID, NON_EXISTENT_STOP));
    }
}