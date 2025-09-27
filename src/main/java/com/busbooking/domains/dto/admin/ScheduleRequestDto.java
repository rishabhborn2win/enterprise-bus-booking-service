package com.busbooking.domains.dto.admin;

import com.busbooking.entities.Bus;
import com.busbooking.entities.Route;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import com.busbooking.entities.Seat;
import com.busbooking.entities.Stop;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

// In: com.bussystem.dto.ScheduleRequestDto
@Data
public class ScheduleRequestDto {

    @Schema(description = "ID of the Bus associated with this schedule.")
    private Long busId;

    @Schema(description = "ID of the Route (Source->Destination).")
    private Long routeId;

    @Schema(description = "Full departure date and time (YYYY-MM-DDTHH:MM:SS).")
    private LocalDateTime departureTime;

    @Schema(description = "Base fare for the full route.")
    private BigDecimal basePrice;

    @Schema(description = "List of stops and their sequence/timing for this schedule.")
    private List<ScheduleStopDto> stops;

    @Schema(description = "List of seat configurations and pricing for this schedule.")
    private List<SeatDto> seats; // 🔑 NEW FIELD

    /**
     * Converts DTO to the basic Schedule entity. NOTE: Dependent entities (Bus, Route) are stubbed
     * here and MUST be fetched by AdminService.
     *
     * @return A Schedule entity populated with DTO fields.
     */
    public Schedule toScheduleEntity() {
        Schedule schedule = new Schedule();

        // --- Schedule Fields ---
        schedule.setDepartureTime(this.departureTime);
        schedule.setBasePrice(this.basePrice);

        // --- Foreign Key Stubs ---
        // The service layer must fetch the full entities and replace these stubs
        schedule.setBus(Bus.builder().id(this.busId).build());
        schedule.setRoute(Route.builder().id(this.routeId).build());

        // Note: The 'stops' list is populated by toScheduleStopEntities() later
        return schedule;
    }

    /**
     * Converts the nested list of ScheduleStopDtos into ScheduleStop entities.
     *
     * @param schedule The parent Schedule entity to which these stops belong.
     * @return A list of ScheduleStop entities.
     */
    public List<ScheduleStop> toScheduleStopEntities(Schedule schedule) {
        if (this.stops == null) {
            return List.of();
        }

        validateStopsOrder();

        return this.stops.stream()
                .map(
                        dto -> {
                            ScheduleStop scheduleStop = new ScheduleStop();
                            scheduleStop.setSchedule(schedule);
                            scheduleStop.setStopOrder(dto.getStopOrder());
                            scheduleStop.setArrivalTime(dto.getArrivalTime());

                            // --- Foreign Key Stubs ---
                            // The service layer must ensure the Stop entity is valid via stopId
                            scheduleStop.setStop(Stop.builder().id(dto.getStopId()).build());

                            return scheduleStop;
                        })
                .collect(Collectors.toList());
    }

    // Add this method to ScheduleRequestDto
    public void validateStopsOrder() {
        if (stops == null || stops.isEmpty()) return;

        for (int i = 1; i < stops.size(); i++) {
            ScheduleStopDto prev = stops.get(i - 1);
            ScheduleStopDto curr = stops.get(i);

            if (curr.getStopOrder() <= prev.getStopOrder()) {
                throw new IllegalArgumentException("Stop order must be strictly increasing.");
            }
            if (curr.getArrivalTime().isBefore(prev.getArrivalTime())
                    || curr.getArrivalTime().isEqual(prev.getArrivalTime())) {
                throw new IllegalArgumentException("Arrival time must be strictly increasing.");
            }
        }
    }

    /**
     * Converts the nested list of SeatDtos into Seat entities.
     *
     * @param schedule The parent Schedule entity to which these seats belong.
     * @return A list of Seat entities.
     */
    public List<Seat> toSeatEntities(Schedule schedule) {
        if (this.seats == null) {
            return List.of();
        }

        return this.seats.stream()
                .map(
                        dto -> {
                            Seat seat = new Seat();
                            seat.setSchedule(schedule); // Links to the parent schedule
                            seat.setSeatNumber(dto.getSeatNumber());
                            seat.setSeatClass(dto.getSeatClass());
                            seat.setMultiplier(dto.getPriceMultiplier());
                            return seat;
                        })
                .collect(Collectors.toList());
    }
}
