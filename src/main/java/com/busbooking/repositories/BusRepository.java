package com.busbooking.repositories;

import com.busbooking.entities.Bus;
import com.busbooking.entities.Schedule;
import com.busbooking.entities.ScheduleStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository public interface BusRepository extends JpaRepository<Bus, Long> {
}
