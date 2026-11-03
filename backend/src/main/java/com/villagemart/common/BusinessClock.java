package com.villagemart.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Business "today" is always evaluated in the configured business timezone (Asia/Kolkata by default).
 */
@Component
@RequiredArgsConstructor
public class BusinessClock {

    private final Clock clock;

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public Instant now() {
        return clock.instant();
    }

    public ZoneId zone() {
        return clock.getZone();
    }
}
