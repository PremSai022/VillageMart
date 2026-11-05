package com.villagemart.settings;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {

    Optional<BusinessSettings> findFirstByOrderByIdAsc();

    /** Row lock on the settings row; used to serialise one-time owner registration. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BusinessSettings s order by s.id asc")
    List<BusinessSettings> lockAll();
}
