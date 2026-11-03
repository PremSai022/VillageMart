package com.villagemart.settings;

import com.villagemart.settings.BusinessSettingsDtos.BusinessSettingsDto;
import com.villagemart.settings.BusinessSettingsDtos.UpdateBusinessSettingsRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Settings")
@RestController
@RequestMapping("/api/v1/settings/business")
@RequiredArgsConstructor
public class BusinessSettingsController {

    private final BusinessSettingsService service;

    @GetMapping
    public BusinessSettingsDto get() {
        return service.get();
    }

    @PutMapping
    public BusinessSettingsDto update(@Valid @RequestBody UpdateBusinessSettingsRequest request) {
        return service.update(request);
    }
}
