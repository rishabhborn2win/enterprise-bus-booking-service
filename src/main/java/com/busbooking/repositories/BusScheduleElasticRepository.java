package com.busbooking.repositories;

import com.busbooking.domains.dto.BusScheduleDocument;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BusScheduleElasticRepository
        extends ElasticsearchRepository<BusScheduleDocument, Long> {
    Page<BusScheduleDocument> findBySourceStopIdAndDestinationStopIdAndDepartureTimeBetween(
            Long sourceStopId,
            Long destinationStopId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            Pageable pageable);
}
