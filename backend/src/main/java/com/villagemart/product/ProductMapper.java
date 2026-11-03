package com.villagemart.product;

import com.villagemart.inventory.Inventory;
import com.villagemart.product.ProductDtos.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ProductMapper {

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", source = "product.updatedAt")
    @Mapping(target = "categoryId", source = "product.category.id")
    @Mapping(target = "categoryName", source = "product.category.name")
    @Mapping(target = "unitId", source = "product.unit.id")
    @Mapping(target = "unitCode", source = "product.unit.code")
    @Mapping(target = "unitName", source = "product.unit.name")
    @Mapping(target = "unitAllowsDecimal", source = "product.unit.allowsDecimal")
    @Mapping(target = "active", source = "product.active")
    @Mapping(target = "availableQuantity", source = "inventory.quantity")
    @Mapping(target = "averageCost", source = "inventory.averageCost")
    @Mapping(target = "stockStatus", source = "inventory.stockStatus")
    ProductDto toDto(Product product, Inventory inventory);
}
