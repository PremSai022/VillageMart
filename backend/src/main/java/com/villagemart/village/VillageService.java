package com.villagemart.village;

import com.villagemart.common.ApiException;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.village.VillageDtos.VillageDto;
import com.villagemart.village.VillageDtos.VillageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class VillageService {

    private static final Set<String> SORTABLE = Set.of("name", "district", "createdAt");

    private final VillageRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<VillageDto> search(String q, Boolean active, int page, int size, String sort) {
        String like = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
        return PageResponse.of(repository.search(like, active, Pages.of(page, size, sort, SORTABLE, Sort.by("name"))),
                VillageService::toDto);
    }

    @Transactional(readOnly = true)
    public VillageDto getDto(Long id) {
        return toDto(get(id));
    }

    @Transactional
    public VillageDto create(VillageRequest r) {
        Village v = new Village();
        apply(v, r);
        return toDto(repository.save(v));
    }

    @Transactional
    public VillageDto update(Long id, VillageRequest r) {
        Village v = get(id);
        apply(v, r);
        return toDto(v);
    }

    @Transactional(readOnly = true)
    public Village get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Village", id));
    }

    private void apply(Village v, VillageRequest r) {
        String name = r.name().trim();
        String district = r.district() == null || r.district().isBlank() ? null : r.district().trim();
        repository.findSame(name, district).stream()
                .filter(existing -> !existing.getId().equals(v.getId()))
                .findAny()
                .ifPresent(existing -> {
                    throw ApiException.conflict("DUPLICATE_VILLAGE", "Village " + name
                            + (district == null ? "" : " (" + district + ")") + " already exists");
                });
        v.setName(name);
        v.setDistrict(district);
        v.setState(r.state().trim());
        v.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        if (r.active() != null) {
            v.setActive(r.active());
        }
    }

    public static VillageDto toDto(Village v) {
        return new VillageDto(v.getId(), v.getName(), v.getDistrict(), v.getState(), v.getNotes(), v.isActive(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
