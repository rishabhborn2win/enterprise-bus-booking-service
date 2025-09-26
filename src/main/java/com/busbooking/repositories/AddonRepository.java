package com.busbooking.repositories;

import com.busbooking.entities.Addon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository public interface AddonRepository extends JpaRepository<Addon, Integer> {}
