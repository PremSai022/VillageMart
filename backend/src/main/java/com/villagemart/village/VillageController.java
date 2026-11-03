package com.villagemart.village;

import com.villagemart.common.PageResponse;
import com.villagemart.village.VillageDtos.VillageDto;
import com.villagemart.village.VillageDtos.VillageRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Villages")
@RestController
@RequestMapping("/api/v1/villages")
@RequiredArgsConstructor
public class VillageController {

    private final VillageService service;

    @GetMapping
    public PageResponse<VillageDto> list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Boolean active,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size,
                                         @RequestParam(required = false) String sort) {
        return service.search(q, active, page, size, sort);
    }

    @GetMapping("/{id}")
    public VillageDto get(@PathVariable Long id) {
        return service.getDto(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VillageDto create(@Valid @RequestBody VillageRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public VillageDto update(@PathVariable Long id, @Valid @RequestBody VillageRequest request) {
        return service.update(id, request);
    }
}
