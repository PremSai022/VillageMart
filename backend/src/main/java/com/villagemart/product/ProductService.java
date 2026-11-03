package com.villagemart.product;

import com.villagemart.category.Category;
import com.villagemart.category.CategoryService;
import com.villagemart.common.ApiException;
import com.villagemart.common.Money;
import com.villagemart.common.PageResponse;
import com.villagemart.common.Pages;
import com.villagemart.inventory.Inventory;
import com.villagemart.inventory.InventoryService;
import com.villagemart.inventory.MovementType;
import com.villagemart.inventory.StockChange;
import com.villagemart.product.ProductDtos.ProductDto;
import com.villagemart.product.ProductDtos.ProductRequest;
import com.villagemart.unit.Unit;
import com.villagemart.unit.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SORTABLE = Set.of("name", "sku", "sellingPrice", "createdAt", "updatedAt");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProductRepository repository;
    private final CategoryService categoryService;
    private final UnitService unitService;
    private final InventoryService inventoryService;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(String q, Long categoryId, Boolean active, int page, int size, String sort) {
        String raw = q == null || q.isBlank() ? null : q.trim();
        String like = raw == null ? null : "%" + raw.toLowerCase() + "%";
        Page<Product> result = repository.search(like, raw, categoryId, active,
                Pages.of(page, size, sort, SORTABLE, Sort.by("name")));
        Map<Long, Inventory> stock = inventoryService.findByProducts(result.getContent().stream().map(Product::getId).toList());
        return PageResponse.of(result, p -> mapper.toDto(p, stock.get(p.getId())));
    }

    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        Product p = getDetailed(id);
        return mapper.toDto(p, inventoryService.findByProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductDto getByBarcode(String barcode) {
        Product p = repository.findByBarcode(barcode.trim())
                .orElseThrow(() -> ApiException.notFound("Product with barcode", barcode));
        return get(p.getId());
    }

    @Transactional
    public ProductDto create(ProductRequest r) {
        Product p = new Product();
        String sku = r.sku() == null || r.sku().isBlank() ? generateSku() : r.sku().trim().toUpperCase();
        ensureUniqueSku(sku, null);
        p.setSku(sku);
        apply(p, r);
        repository.saveAndFlush(p);
        inventoryService.initialize(p);
        if (Money.isPositive(r.openingStock())) {
            UnitService.validateQuantity(p.getUnit(), r.openingStock(), p.getName());
            BigDecimal cost = r.openingStockUnitCost() != null ? r.openingStockUnitCost() : p.getPurchasePrice();
            inventoryService.apply(StockChange.inbound(p.getId(), MovementType.OPENING_STOCK, r.openingStock(), cost,
                    "PRODUCT", p.getId(), p.getSku(), "Opening stock on product creation"));
        }
        return mapper.toDto(p, inventoryService.findByProduct(p.getId()));
    }

    @Transactional
    public ProductDto update(Long id, ProductRequest r) {
        Product p = getDetailed(id);
        if (r.sku() != null && !r.sku().isBlank()) {
            String sku = r.sku().trim().toUpperCase();
            ensureUniqueSku(sku, id);
            p.setSku(sku);
        }
        BigDecimal oldMin = p.getMinStockQuantity();
        apply(p, r);
        repository.flush();
        if (oldMin.compareTo(p.getMinStockQuantity()) != 0) {
            inventoryService.refreshStatus(p.getId());
        }
        return mapper.toDto(p, inventoryService.findByProduct(id));
    }

    @Transactional
    public ProductDto setActive(Long id, boolean active) {
        Product p = getDetailed(id);
        p.setActive(active);
        return mapper.toDto(p, inventoryService.findByProduct(id));
    }

    @Transactional
    public void delete(Long id) {
        Product p = getDetailed(id);
        if (repository.isUsed(id)) {
            throw ApiException.conflict("PRODUCT_IN_USE",
                    "Product '" + p.getName() + "' has stock or transaction history and cannot be deleted. Deactivate it instead.");
        }
        inventoryService.deleteForUnusedProduct(id);
        repository.delete(p);
    }

    @Transactional(readOnly = true)
    public Product getDetailed(Long id) {
        return repository.findDetailed(id).orElseThrow(() -> ApiException.notFound("Product", id));
    }

    private void apply(Product p, ProductRequest r) {
        Category category = categoryService.get(r.categoryId());
        Unit unit = unitService.get(r.unitId());
        String barcode = r.barcode() == null || r.barcode().isBlank() ? null : r.barcode().trim();
        if (barcode != null) {
            repository.findByBarcode(barcode).ifPresent(existing -> {
                if (!existing.getId().equals(p.getId())) {
                    throw ApiException.conflict("DUPLICATE_BARCODE", "Barcode " + barcode + " is already used by " + existing.getName());
                }
            });
        }
        if (r.minSellingPrice() != null && r.minSellingPrice().compareTo(r.sellingPrice()) > 0) {
            throw ApiException.business("INVALID_PRICE", "Minimum selling price cannot exceed the default selling price");
        }
        UnitService.validateQuantity(unit, r.minStockQuantity(), r.name());
        p.setName(r.name().trim());
        p.setBarcode(barcode);
        p.setCategory(category);
        p.setUnit(unit);
        p.setDescription(blankToNull(r.description()));
        p.setImageUrl(blankToNull(r.imageUrl()));
        p.setPurchasePrice(Money.money(r.purchasePrice()));
        p.setSellingPrice(Money.money(r.sellingPrice()));
        p.setMinSellingPrice(r.minSellingPrice() == null ? null : Money.money(r.minSellingPrice()));
        p.setMinStockQuantity(Money.qty(r.minStockQuantity()));
        p.setTaxRate(r.taxRate() == null ? BigDecimal.ZERO.setScale(2) : r.taxRate().setScale(2, Money.ROUNDING));
        p.setHsnCode(blankToNull(r.hsnCode()));
        if (r.active() != null) {
            p.setActive(r.active());
        }
    }

    private void ensureUniqueSku(String sku, Long currentId) {
        repository.findBySkuIgnoreCase(sku).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw ApiException.conflict("DUPLICATE_SKU", "SKU " + sku + " is already used by " + existing.getName());
            }
        });
    }

    private String generateSku() {
        for (int i = 0; i < 20; i++) {
            String candidate = "VM" + (100000 + RANDOM.nextInt(900000));
            if (repository.findBySkuIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique SKU");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
