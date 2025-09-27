package com.busbooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.busbooking.domains.dto.admin.RouteRequestDto;
import com.busbooking.domains.dto.admin.StopRequestDto;
import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.entities.Seat;
import com.busbooking.entities.Stop;
import com.busbooking.repositories.BusRepository;
import com.busbooking.repositories.RouteRepository;
import com.busbooking.repositories.ScheduleRepository;
import com.busbooking.repositories.ScheduleStopRepository;
import com.busbooking.repositories.SeatRepository;
import com.busbooking.repositories.StopRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ScheduleStopRepository scheduleStopRepository;
    @Mock private BusRepository busRepository;
    @Mock private RouteRepository routeRepository;
    @Mock private StopRepository stopRepository;
    @Mock private SyncService syncService;
    @Mock private SeatRepository seatRepository;

    @InjectMocks private AdminService adminService;

    // --- Mock Data Setup ---
    private Bus mockBus;
    private Route mockRoute;
    private Stop mockStopA;
    private Stop mockStopB;
    private Schedule mockSchedule;
    private LocalDateTime mockTime;

    @BeforeEach
    void setUp() {
        mockTime = LocalDateTime.of(2025, 10, 10, 10, 0);

        mockBus = Bus.builder().id(100L).operator("TestOp").totalSeats(40).build();

        mockStopA = Stop.builder().id(200L).name("CityA").city("A").build();
        mockStopB = Stop.builder().id(201L).name("CityB").city("B").build();

        mockRoute =
                Route.builder()
                        .id(300L)
                        .sourceStop(mockStopA)
                        .destinationStop(mockStopB)
                        .distanceKm(500)
                        .build();

        mockSchedule =
                Schedule.builder()
                        .id(400L)
                        .bus(mockBus)
                        .route(mockRoute)
                        .departureTime(mockTime)
                        .build();
    }

    // ====================================================================
    // 1. Core Transaction and Conflict Logic Tests (createOrUpdateSchedule)
    // ====================================================================

    private ScheduleStop createScheduleStop(Long id, int order, LocalDateTime time) {
        return ScheduleStop.builder().id(id).stopOrder(order).arrivalTime(time).build();
    }

    @Test
    void createOrUpdateSchedule_Success_NoConflict() {
        // Arrange
        LocalDateTime terminationTime = mockTime.plusHours(5);
        List<ScheduleStop> stops =
                List.of(
                        createScheduleStop(501L, 1, mockTime),
                        createScheduleStop(502L, 2, terminationTime));
        List<Seat> seats = List.of(Seat.builder().id(600L).seatNumber("A1").build());

        // Mock dependencies for successful flow
        when(scheduleRepository.findByBusId(anyLong()))
                .thenReturn(Collections.emptyList()); // NO CONFLICT
        when(routeRepository.findById(anyLong())).thenReturn(Optional.of(mockRoute));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(mockSchedule);
        when(scheduleRepository.findById(anyLong())).thenReturn(Optional.of(mockSchedule));

        // Act
        Schedule result = adminService.createOrUpdateSchedule(mockSchedule, stops, seats);

        // Assert
        assertNotNull(result);
        verify(routeRepository, times(1)).findById(mockRoute.getId());
        verify(scheduleRepository, times(1)).findByBusId(mockBus.getId());
        verify(scheduleStopRepository, times(1)).saveAll(stops);
        verify(seatRepository, times(1)).saveAll(seats);
        verify(syncService, times(1)).syncScheduleToElasticsearch(mockSchedule.getId());
    }

    @Test
    void createOrUpdateSchedule_Failure_BusConflict() {
        // Arrange
        LocalDateTime newStartTime = LocalDateTime.of(2025, 10, 10, 12, 0);
        LocalDateTime newTerminationTime =
                LocalDateTime.of(2025, 10, 10, 17, 0); // Window: 12:00 to 17:00

        Schedule existingSchedule =
                Schedule.builder()
                        .id(401L)
                        .bus(mockBus)
                        .departureTime(LocalDateTime.of(2025, 10, 10, 11, 0))
                        .build(); // Starts at 11:00

        // Mock existing schedule stops for conflict check: Ends at 13:00
        List<ScheduleStop> existingStops =
                List.of(
                        createScheduleStop(601L, 1, existingSchedule.getDepartureTime()),
                        createScheduleStop(
                                602L, 2, LocalDateTime.of(2025, 10, 10, 13, 0)) // Termination Time
                        );

        // Mock dependencies for the new schedule
        List<ScheduleStop> newStops =
                List.of(createScheduleStop(501L, 1, newTerminationTime)); // Stops list is large

        // Mock the findByBusId to return a list that will cause a conflict (11:00-13:00 overlaps
        // 12:00-17:00)
        when(scheduleRepository.findByBusId(mockBus.getId())).thenReturn(List.of(existingSchedule));
        when(scheduleStopRepository.findByScheduleId(existingSchedule.getId()))
                .thenReturn(existingStops);
        //        when(routeRepository.findById(anyLong())).thenReturn(Optional.of(mockRoute));

        // Act & Assert
        assertThrows(
                IllegalStateException.class,
                () ->
                        adminService.createOrUpdateSchedule(
                                mockSchedule, newStops, Collections.emptyList()),
                "Should throw IllegalStateException due to bus conflict.");

        // Verify that save operations were NOT called
        verify(scheduleRepository, never()).save(any(Schedule.class));
        verify(seatRepository, never()).saveAll(anyList());
    }

    @Test
    void createOrUpdateSchedule_Update_ClearsOldSeatsAndStops() {
        // Arrange: Schedule already exists (ID 400L)
        List<ScheduleStop> newStops = List.of(createScheduleStop(503L, 1, mockTime.plusHours(2)));
        List<Seat> newSeats = List.of(Seat.builder().id(601L).seatNumber("Z1").build());

        // Mock dependencies for successful flow
        when(scheduleRepository.findByBusId(anyLong())).thenReturn(Collections.emptyList());
        when(routeRepository.findById(anyLong())).thenReturn(Optional.of(mockRoute));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(mockSchedule);
        when(scheduleRepository.findById(anyLong())).thenReturn(Optional.of(mockSchedule));

        // Act
        adminService.createOrUpdateSchedule(mockSchedule, newStops, newSeats);

        // Verify new data was saved
        verify(scheduleStopRepository, times(1)).saveAll(newStops);
        verify(seatRepository, times(1)).saveAll(newSeats);
    }

    // ====================================================================
    // 2. Stop Management Tests
    // ====================================================================

    @Test
    void saveStop_Success() {
        // Arrange
        StopRequestDto request = new StopRequestDto();
        request.setName("NewStop");
        request.setCity("NewCity");
        Stop stopToSave = request.toEntity();

        when(stopRepository.save(any(Stop.class))).thenReturn(stopToSave);

        // Act
        Stop result = adminService.saveStop(request);

        // Assert
        assertNotNull(result);
        verify(stopRepository, times(1)).save(any(Stop.class));
    }

    @Test
    void updateStop_Success() {
        // Arrange
        Long stopId = 200L;
        StopRequestDto request = new StopRequestDto();
        request.setName("UpdatedName");
        request.setCity("UpdatedCity");

        when(stopRepository.findById(stopId)).thenReturn(Optional.of(mockStopA));
        when(stopRepository.save(any(Stop.class))).thenReturn(mockStopA);

        // Act
        Stop result = adminService.updateStop(stopId, request);

        // Assert
        assertEquals("UpdatedName", result.getName());
        assertEquals("UpdatedCity", result.getCity());
        verify(stopRepository, times(1)).save(mockStopA);
    }

    @Test
    void updateStop_NotFound_ThrowsException() {
        // Arrange
        Long stopId = 999L;
        when(stopRepository.findById(stopId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                NoSuchElementException.class,
                () -> adminService.updateStop(stopId, new StopRequestDto()));
    }

    // ====================================================================
    // 3. Route Management Tests
    // ====================================================================

    @Test
    void createOrUpdateRoute_Success() {
        // Arrange
        RouteRequestDto request = new RouteRequestDto();
        request.setSourceStopId(mockStopA.getId());
        request.setDestStopId(mockStopB.getId());
        request.setDistanceKm(100);

        // Mock dependency fetching
        when(stopRepository.findById(mockStopA.getId())).thenReturn(Optional.of(mockStopA));
        when(stopRepository.findById(mockStopB.getId())).thenReturn(Optional.of(mockStopB));
        when(routeRepository.save(any(Route.class))).thenReturn(mockRoute);

        // Act
        Route result = adminService.createOrUpdateRoute(request);

        // Assert
        assertNotNull(result);
        verify(routeRepository, times(1)).save(any(Route.class));
    }

    @Test
    void createOrUpdateRoute_InvalidStops_ThrowsException() {
        // Arrange: Source and Destination are the same
        RouteRequestDto request = new RouteRequestDto();
        request.setSourceStopId(mockStopA.getId());
        request.setDestStopId(mockStopA.getId());

        when(stopRepository.findById(mockStopA.getId())).thenReturn(Optional.of(mockStopA));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class, () -> adminService.createOrUpdateRoute(request));
    }
}
