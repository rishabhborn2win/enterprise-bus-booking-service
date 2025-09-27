package com.busbooking.services;

import com.busbooking.domains.dto.SeatAvailabilityResponse;
import com.busbooking.entities.Seat;
import com.busbooking.enums.SeatClass;
import com.busbooking.repositories.BookingSeatRepository;
import com.busbooking.repositories.ScheduleStopRepository;
import com.busbooking.repositories.SeatRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ** In: com.bussystem.service.SeatAvailabilityService **
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {
    private final ScheduleStopRepository scheduleStopRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public SeatAvailabilityResponse getSegmentAvailability(
            Long scheduleId, Long sourceStopId, Long destinationStopId) {
        // 1. Get Stop Orders for the requested query segment (X -> Y)
        Integer queryStartOrder =
                scheduleStopRepository
                        .findStopOrderByScheduleIdAndStopId(scheduleId, sourceStopId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Source stop not valid for schedule."));
        Integer queryEndOrder =
                scheduleStopRepository
                        .findStopOrderByScheduleIdAndStopId(scheduleId, destinationStopId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Destination stop not valid for schedule."));

        // Validation: X must be before Y
        if (queryStartOrder >= queryEndOrder) {
            throw new IllegalArgumentException("Invalid segment: Source must precede destination.");
        }

        // 2. Fetch all booked seat segments (with their stop orders) for the schedule.
        List<Object[]> bookedSegments =
                bookingSeatRepository.findConfirmedBookedSeatSegmentsWithOrders(scheduleId);

        // 3. Identify Unavailable Seats via Overlap Logic
        Set<Long> unavailableSeatIds = new HashSet<>();
        for (Object[] segment : bookedSegments) {
            Long bookedSeatId = ((Number) segment[0]).longValue();
            int bookedStartOrder = ((Number) segment[1]).intValue();
            int bookedEndOrder = ((Number) segment[2]).intValue();

            // Overlap Condition: (Booked Start < Query End) AND (Booked End > Query Start)
            boolean overlaps =
                    (bookedStartOrder < queryEndOrder) && (bookedEndOrder > queryStartOrder);

            if (overlaps) {
                unavailableSeatIds.add(bookedSeatId);
            }
        }

        // 4. Map Seat IDs to Numbers and Classes
        List<Seat> allSeats = seatRepository.findByScheduleId(scheduleId);
        int totalCapacity = allSeats.size();

        Map<String, SeatClass> seatMap =
                allSeats.stream()
                        .collect(Collectors.toMap(Seat::getSeatNumber, Seat::getSeatClass));

        Set<String> bookedSeatNumbers =
                allSeats.stream()
                        .filter(seat -> unavailableSeatIds.contains(seat.getId()))
                        .map(Seat::getSeatNumber)
                        .collect(Collectors.toSet());

        Set<String> availableSeatNumbers =
                allSeats.stream().map(Seat::getSeatNumber).collect(Collectors.toSet());

        availableSeatNumbers.removeAll(bookedSeatNumbers);

        return new SeatAvailabilityResponse(
                totalCapacity,
                bookedSeatNumbers.stream().toList(),
                availableSeatNumbers.stream().toList(),
                seatMap);
    }
}
