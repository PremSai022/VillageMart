package com.villagemart.unit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UnitDtos {

    private UnitDtos() {
    }

    public record UnitDto(Long id, String code, String name, boolean allowsDecimal) {
    }

    public record UnitRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z]{1,20}$", message = "Code must be 1-20 letters") String code,
            @NotBlank @Size(max = 40) String name,
            boolean allowsDecimal) {
    }
}
