package com.busbooking.repositories;

import com.busbooking.domains.dto.BusScheduleDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BusScheduleElasticRepository {
    Page<BusScheduleDocument> findBySourceStopIdAndDestinationStopIdAndDepartureTimeBetween(
        Long sourceStopId, 
        Long destinationStopId, 
        LocalDateTime startOfDay, 
        LocalDateTime endOfDay, 
        Pageable pageable
    );
}