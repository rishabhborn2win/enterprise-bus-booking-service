package com.busbooking.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.busbooking.domains.dto.BusScheduleDocument;
import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.entities.Stop;
import com.busbooking.repositories.BusScheduleElasticRepository;
import com.busbooking.repositories.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock private ScheduleRepository scheduleRepository;

    @Mock private BusScheduleElasticRepository elasticRepository;

    @InjectMocks private SyncService syncService;

    // --- Mock Data Setup ---
    private Schedule mockSchedule;
    private LocalDateTime mockDepartureTime;
    private LocalDateTime mockArrivalTime;
    private final Long SCHEDULE_ID = 400L;

    @BeforeEach
    void setUp() {
        mockDepartureTime = LocalDateTime.of(2025, 12, 10, 10, 0);
        mockArrivalTime = LocalDateTime.of(2025, 12, 10, 15, 0);

        Bus mockBus = Bus.builder().id(100L).operator("RoyalBuses").totalSeats(45).build();
        Stop mockSource = Stop.builder().id(200L).name("Mumbai").city("Mumbai").build();
        Stop mockDest = Stop.builder().id(201L).name("Pune").city("Pune").build();
        Route mockRoute =
                Route.builder().id(300L).sourceStop(mockSource).destinationStop(mockDest).build();

        ScheduleStop mockStop1 =
                ScheduleStop.builder()
                        .id(501L)
                        .stop(mockSource)
                        .stopOrder(1)
                        .arrivalTime(mockDepartureTime)
                        .build();

        ScheduleStop mockStop2 =
                ScheduleStop.builder()
                        .id(502L)
                        .stop(mockDest)
                        .stopOrder(2)
                        .arrivalTime(mockArrivalTime)
                        .build();

        mockSchedule =
                Schedule.builder()
                        .id(SCHEDULE_ID)
                        .bus(mockBus)
                        .route(mockRoute)
                        .departureTime(mockDepartureTime)
                        .basePrice(BigDecimal.valueOf(1500.00))
                        .stops(List.of(mockStop1, mockStop2)) // Use correct list name
                        .build();
    }

    // ====================================================================
    // 1. mapToDocument Logic Tests
    // ====================================================================

    @Test
    void mapToDocument_ShouldMapAllFieldsCorrectly() {
        // ACT
        BusScheduleDocument document = syncService.mapToDocument(mockSchedule);

        // ASSERT
        assertNotNull(document);
        assertEquals(SCHEDULE_ID, document.getId());
        assertEquals(100L, document.getBusId());
        assertEquals("RoyalBuses", document.getOperator());
        assertEquals(45, document.getTotalCapacity());
        assertEquals(200L, document.getSourceStopId());
        assertEquals(201L, document.getDestinationStopId());
        assertEquals(mockDepartureTime, document.getDepartureTime());

        // Assert nested stops list is populated
        assertFalse(document.getStops().isEmpty());
        assertEquals(2, document.getStops().size());

        // Assert nested stop details (Time mapping check)
        BusScheduleDocument.ScheduleStopDetail finalStop =
                document.getStops().stream()
                        .filter(d -> d.getStopOrder() == 2)
                        .findFirst()
                        .orElseThrow();

        // Check that the correct time format was used in the DTO
        // The service code converts to Epoch Second, so we check the value.
        assertEquals(mockArrivalTime.toEpochSecond(ZoneOffset.UTC), finalStop.getArrivalTime());
        assertEquals("Pune", finalStop.getStopName());
    }

    @Test
    void mapToDocument_ShouldHandleEmptyStopsList() {
        // Arrange
        mockSchedule.setStops(Collections.emptyList());

        // ACT
        BusScheduleDocument document = syncService.mapToDocument(mockSchedule);

        // ASSERT
        assertNotNull(document);
        assertTrue(document.getStops().isEmpty());
    }

    // ====================================================================
    // 2. Control Method Tests
    // ====================================================================

    @Test
    void syncScheduleToElasticsearch_Success() {
        // Arrange
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(mockSchedule));

        // ACT
        syncService.syncScheduleToElasticsearch(SCHEDULE_ID);

        // ASSERT
        verify(scheduleRepository, times(1)).findById(SCHEDULE_ID);
        // Verify that the save operation was called on the Elasticsearch repository
        verify(elasticRepository, times(1)).save(any(BusScheduleDocument.class));
    }

    @Test
    void syncScheduleToElasticsearch_ScheduleNotFound_ThrowsException() {
        // Arrange
        when(scheduleRepository.findById(anyLong())).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(
                EntityNotFoundException.class, () -> syncService.syncScheduleToElasticsearch(999L));

        // Verify no save attempt was made on Elasticsearch
        verify(elasticRepository, never()).save(any());
    }

    @Test
    void performFullSync_Success_WithData() {
        // Arrange
        when(scheduleRepository.findAll()).thenReturn(List.of(mockSchedule));

        // ACT
        syncService.performFullSync();

        // ASSERT
        verify(scheduleRepository, times(1)).findAll();
        // Verify saveAll was called for bulk indexing
        verify(elasticRepository, times(1)).saveAll(anyList());
    }

    @Test
    void performFullSync_NoData() {
        // Arrange
        when(scheduleRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        syncService.performFullSync();

        // ASSERT
        verify(scheduleRepository, times(1)).findAll();
        // Verify saveAll was called (even with an empty list, but no actual documents processed)
        verify(elasticRepository, never()).saveAll(Collections.emptyList());
    }

    @Test
    void deleteScheduleFromElasticsearch_Success() {
        // ACT
        syncService.deleteScheduleFromElasticsearch(SCHEDULE_ID);

        // ASSERT
        verify(elasticRepository, times(1)).deleteById(SCHEDULE_ID);
    }
}
