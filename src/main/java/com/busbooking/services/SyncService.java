package com.busbooking.services;

import com.busbooking.domains.dto.BusScheduleDocument;
import com.busbooking.entities.Schedule;
import com.busbooking.repositories.BusScheduleElasticRepository;
import com.busbooking.repositories.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    private final ScheduleRepository scheduleRepository;
    private final BusScheduleElasticRepository elasticRepository;

    @Transactional(readOnly = true)
    public BusScheduleDocument mapToDocument(Schedule schedule) {
        BusScheduleDocument doc = new BusScheduleDocument();

        // Set direct fields
        doc.setId(schedule.getId());
        doc.setTotalCapacity(schedule.getBus().getTotalSeats());
        doc.setDepartureTime(schedule.getDepartureTime());
        doc.setBasePrice(schedule.getBasePrice());
        doc.setOperator(schedule.getBus().getOperator()); // Assuming this is set on the Bus entity

        // Set Route fields (via Schedule relationships)
        doc.setRouteId(schedule.getRoute().getId());
        doc.setBusId(schedule.getBus().getId());
        doc.setSourceStopId(schedule.getRoute().getSourceStop().getId());
        doc.setDestinationStopId(schedule.getRoute().getDestinationStop().getId());

        // --- CRITICAL FIX 1: Correctly accessing the Schedule Stops relationship ---
        // The relationship is named 'scheduleStops' in the Schedule entity.
        List<BusScheduleDocument.ScheduleStopDetail> stopDetails =
                schedule.getStops().stream() // CORRECTED: Use getScheduleStops()
                        .map(
                                ss -> {
                                    BusScheduleDocument.ScheduleStopDetail detail =
                                            new BusScheduleDocument.ScheduleStopDetail();

                                    // Set direct ScheduleStop fields
                                    detail.setStopId(ss.getStop().getId());
                                    detail.setStopName(ss.getStop().getName());
                                    detail.setStopOrder(ss.getStopOrder());

                                    // --- CRITICAL FIX 2: Correct Time Mapping ---
                                    // Elasticsearch mapping often expects the ISO standard string.
                                    // We rely on Spring Data ES to convert LocalDateTime, but
                                    // for maximum compatibility, we ensure the correct Java time
                                    // object is passed.
                                    detail.setArrivalTime(
                                            ss.getArrivalTime().toEpochSecond(ZoneOffset.UTC));

                                    return detail;
                                })
                        .toList();

        doc.setStops(stopDetails);

        return doc;
    }

    public void syncScheduleToElasticsearch(Long scheduleId) {
        Schedule schedule =
                scheduleRepository
                        .findById(scheduleId)
                        // Use explicit fetching/loading if relationships aren't eagerly fetched by
                        // default.
                        .orElseThrow(
                                () -> new EntityNotFoundException("Schedule not found for sync."));

        BusScheduleDocument document = mapToDocument(schedule);
        elasticRepository.save(document);
    }

    /**
     * Requirement: Manually Trigger Full Sync. Fetches all schedules from MySQL and performs a bulk
     * save/update to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void performFullSync() {
        log.info("Starting full synchronization of all schedules from MySQL to Elasticsearch...");

        // 1. Fetch all schedules. Use a method optimized for fetching related data (e.g., a custom
        // query
        // with JOIN FETCH for Bus, Route, and ScheduleStops to avoid N+1 problems during mapping).
        // For simplicity here, we assume the JPA repository manages the fetching well:
        List<Schedule> allSchedules = scheduleRepository.findAll();

        // 2. Map all JPA entities to Elasticsearch Documents
        List<BusScheduleDocument> documents =
                allSchedules.stream().map(this::mapToDocument).toList();

        // 3. Save all to Elasticsearch in bulk
        if (!documents.isEmpty()) {
            elasticRepository.saveAll(documents);
            log.info(
                    "Full sync completed successfully. {} schedules indexed into Elasticsearch.",
                    documents.size());
        } else {
            log.warn("Full sync completed. No schedules found in MySQL to index.");
        }
    }

    /** REQUIRED IMPLEMENTATION: Deletes a schedule document from the Elasticsearch index. */
    public void deleteScheduleFromElasticsearch(Long scheduleId) {
        // The scheduleId is the document's ID in Elasticsearch
        elasticRepository.deleteById(scheduleId);
        log.info("Schedule ID {} successfully deleted from Elasticsearch index.", scheduleId);
    }
}
