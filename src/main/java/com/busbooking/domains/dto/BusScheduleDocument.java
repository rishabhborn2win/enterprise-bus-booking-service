package com.busbooking.domains.dto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "bus_schedules")
@Data
public class BusScheduleDocument {
    @Id
    private Long id; // Schedule ID
    @Field(type = FieldType.Long) private Long routeId;
    @Field(type = FieldType.Long) private Long busId;
    @Field(type = FieldType.Long) private Long sourceStopId;
    @Field(type = FieldType.Long) private Long destinationStopId;
    @Field(type = FieldType.Text) private String operator; // For searching/filtering
    @Field(type = FieldType.Integer) private Integer totalCapacity;
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime departureTime;
    @Field(type = FieldType.Double) private BigDecimal basePrice;
    
    // Nested stops for potential advanced search (e.g., stops near a location)
    @Field(type = FieldType.Nested, includeInParent = true)
    private List<ScheduleStopDetail> stops;
    
    @Data public static class ScheduleStopDetail {
        private Long stopId;
        private String stopName;
        private LocalDateTime arrivalTime;
        private Integer stopOrder;
    }
}

