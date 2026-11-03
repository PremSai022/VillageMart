package com.villagemart.village;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class VillageDtos {

    private VillageDtos() {
    }

    public record VillageDto(Long id, String name, String district, String state, String notes, boolean active,
                             Instant createdAt, Instant updatedAt) {
    }

    public record VillageRequest(
            @NotBlank(message = "Village name is required") @Size(max = 100) String name,
            @Size(max = 100) String district,
            @NotBlank(message = "State is required") @Size(max = 60) String state,
            @Size(max = 500) String notes,
            Boolean active) {
    }
}
