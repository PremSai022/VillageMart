package com.villagemart.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockLocationRepository extends JpaRepository<StockLocation, Long> {

    Optional<StockLocation> findFirstByDefaultLocationTrue();
}
