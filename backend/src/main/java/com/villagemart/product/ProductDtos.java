package com.villagemart.product;

import com.villagemart.inventory.StockStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductDto(
            Long id, String name, String sku, String barcode,
            Long categoryId, String categoryName,
            Long unitId, String unitCode, String unitName, boolean unitAllowsDecimal,
            String description, String imageUrl,
            BigDecimal purchasePrice, BigDecimal sellingPrice, BigDecimal minSellingPrice,
            BigDecimal minStockQuantity, BigDecimal taxRate, String hsnCode, boolean active,
            BigDecimal availableQuantity, BigDecimal averageCost, StockStatus stockStatus,
            Instant createdAt, Instant updatedAt) {
    }

    public record ProductRequest(
            @NotBlank(message = "Product name is required") @Size(max = 150) String name,
            @Size(max = 50) @Pattern(regexp = "^$|^[A-Za-z0-9\\-_/.]+$", message = "SKU may contain letters, digits, - _ / .") String sku,
            @Size(max = 50) @Pattern(regexp = "^$|^[A-Za-z0-9]+$", message = "Barcode must be alphanumeric") String barcode,
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Unit is required") Long unitId,
            @Size(max = 500) String description,
            @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "Image URL must start with http(s)://") String imageUrl,
            @NotNull @DecimalMin(value = "0.00") @Digits(integer = 12, fraction = 2) BigDecimal purchasePrice,
            @NotNull @DecimalMin(value = "0.01", message = "Selling price must be greater than zero") @Digits(integer = 12, fraction = 2) BigDecimal sellingPrice,
            @DecimalMin(value = "0.00") @Digits(integer = 12, fraction = 2) BigDecimal minSellingPrice,
            @NotNull @DecimalMin(value = "0.000") @Digits(integer = 11, fraction = 3) BigDecimal minStockQuantity,
            @DecimalMin("0.00") @DecimalMax("28.00") @Digits(integer = 2, fraction = 2) BigDecimal taxRate,
            @Size(max = 10) String hsnCode,
            Boolean active,
            @DecimalMin(value = "0.000") @Digits(integer = 11, fraction = 3) BigDecimal openingStock,
            @DecimalMin(value = "0.00") @Digits(integer = 12, fraction = 4) BigDecimal openingStockUnitCost) {
    }

    public record ProductStatusRequest(@NotNull Boolean active) {
    }
}
