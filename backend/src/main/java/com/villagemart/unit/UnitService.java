package com.villagemart.unit;

import com.villagemart.common.ApiException;
import com.villagemart.unit.UnitDtos.UnitDto;
import com.villagemart.unit.UnitDtos.UnitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitService {

    private final UnitRepository repository;

    @Transactional(readOnly = true)
    public List<UnitDto> list() {
        return repository.findAll(Sort.by("id")).stream().map(UnitService::toDto).toList();
    }

    @Transactional
    public UnitDto create(UnitRequest r) {
        String code = r.code().trim().toUpperCase();
        if (repository.findByCodeIgnoreCase(code).isPresent()) {
            throw ApiException.conflict("DUPLICATE_UNIT", "Unit " + code + " already exists");
        }
        Unit u = new Unit();
        u.setCode(code);
        u.setName(r.name().trim());
        u.setAllowsDecimal(r.allowsDecimal());
        return toDto(repository.save(u));
    }

    @Transactional(readOnly = true)
    public Unit get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Unit", id));
    }

    /** Rejects fractional quantities for units such as Piece or Packet. */
    public static void validateQuantity(Unit unit, BigDecimal quantity, String productName) {
        if (!unit.isAllowsDecimal() && quantity.stripTrailingZeros().scale() > 0) {
            throw ApiException.business("FRACTIONAL_QUANTITY_NOT_ALLOWED",
                    "Quantity for " + productName + " must be a whole number of " + unit.getName());
        }
    }

    static UnitDto toDto(Unit u) {
        return new UnitDto(u.getId(), u.getCode(), u.getName(), u.isAllowsDecimal());
    }
}
