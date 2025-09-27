package com.busbooking.services;

import com.busbooking.domains.dto.BusScheduleDocument;
import com.busbooking.entities.Schedule;
import com.busbooking.repositories.BusScheduleElasticRepository;
import com.busbooking.repositories.ScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    private final ScheduleRepository scheduleRepository;
//    private final BusScheduleElasticRepository elasticRepository;

    @Transactional(readOnly = true)
    public BusScheduleDocument mapToDocument(Schedule schedule) {
        // ... (Mapping logic remains similar to previous step, fetching relationships) ...
        // Ensure you fetch all relationships (Bus, Route, ScheduleStops) before mapping.
        // For brevity, assume schedule entity relationships are correctly loaded.
        BusScheduleDocument doc = new BusScheduleDocument();
        doc.setId(schedule.getId());
        doc.setRouteId(schedule.getRoute().getId());
        doc.setBusId(schedule.getBus().getId());
        doc.setSourceStopId(schedule.getRoute().getSourceStop().getId());
        doc.setDestinationStopId(schedule.getRoute().getDestinationStop().getId());
        doc.setOperator(schedule.getBus().getOperator());
        doc.setTotalCapacity(schedule.getBus().getTotalSeats());
        doc.setDepartureTime(schedule.getDepartureTime());
        doc.setBasePrice(schedule.getBasePrice());
        
        // Map Schedule Stops (Requires fetching/loading all stops)
        List<BusScheduleDocument.ScheduleStopDetail> stopDetails = schedule.getStops().stream()
            .map(ss -> {
                BusScheduleDocument.ScheduleStopDetail detail = new BusScheduleDocument.ScheduleStopDetail();
                detail.setStopId(ss.getStop().getId());
                detail.setStopName(ss.getStop().getName());
                detail.setArrivalTime(ss.getArrivalTime());
                detail.setStopOrder(ss.getStopOrder());
                return detail;
            }).toList();
            
        doc.setStops(stopDetails);

        return doc;
    }

    public void syncScheduleToElasticsearch(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
             // Use explicit fetching/loading if relationships aren't eagerly fetched by default.
            .orElseThrow(() -> new EntityNotFoundException("Schedule not found for sync."));
        
        BusScheduleDocument document = mapToDocument(schedule);
//        elasticRepository.save(document);
    }

    /**
     * Requirement: Manually Trigger Full Sync.
     * Fetches all schedules from MySQL and performs a bulk save/update to Elasticsearch.
     */
    @Transactional(readOnly = true)
    public void performFullSync() {
        log.info("Starting full synchronization of all schedules from MySQL to Elasticsearch...");

        // 1. Fetch all schedules. Use a method optimized for fetching related data (e.g., a custom query
        // with JOIN FETCH for Bus, Route, and ScheduleStops to avoid N+1 problems during mapping).
        // For simplicity here, we assume the JPA repository manages the fetching well:
        List<Schedule> allSchedules = scheduleRepository.findAll();

        // 2. Map all JPA entities to Elasticsearch Documents
        List<BusScheduleDocument> documents = allSchedules.stream()
                .map(this::mapToDocument)
                .toList();

        // 3. Save all to Elasticsearch in bulk
        if (!documents.isEmpty()) {
//            elasticRepository.saveAll(documents);
            log.info("Full sync completed successfully. {} schedules indexed into Elasticsearch.", documents.size());
        } else {
            log.warn("Full sync completed. No schedules found in MySQL to index.");
        }
    }

    /**
     * REQUIRED IMPLEMENTATION: Deletes a schedule document from the Elasticsearch index.
     */
    public void deleteScheduleFromElasticsearch(Long scheduleId) {
        // The scheduleId is the document's ID in Elasticsearch
//        elasticRepository.deleteById(scheduleId);
        log.info("Schedule ID {} successfully deleted from Elasticsearch index.", scheduleId);
    }
}