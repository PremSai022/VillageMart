package com.villagemart.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            select p from Product p join fetch p.category c join fetch p.unit u
            where (:q is null or lower(p.name) like :q or lower(p.sku) like :q or p.barcode = :raw)
              and (:categoryId is null or c.id = :categoryId)
              and (:active is null or p.active = :active)
            """,
            countQuery = """
            select count(p) from Product p
            where (:q is null or lower(p.name) like :q or lower(p.sku) like :q or p.barcode = :raw)
              and (:categoryId is null or p.category.id = :categoryId)
              and (:active is null or p.active = :active)
            """)
    Page<Product> search(String q, String raw, Long categoryId, Boolean active, Pageable pageable);

    @Query("select p from Product p join fetch p.category join fetch p.unit where p.id = :id")
    Optional<Product> findDetailed(Long id);

    @Query("select p from Product p join fetch p.category join fetch p.unit where p.id in :ids")
    List<Product> findAllDetailed(Collection<Long> ids);

    Optional<Product> findBySkuIgnoreCase(String sku);

    Optional<Product> findByBarcode(String barcode);

    long countByCategoryId(Long categoryId);

    long countByActiveTrue();

    @Query("select p.category.id, count(p) from Product p group by p.category.id")
    List<Object[]> countGroupedByCategory();

    default Map<Long, Long> countByCategory() {
        return countGroupedByCategory().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    @Query("select count(m) from StockMovement m where m.product.id = :id")
    long movementCount(Long id);

    @Query("select count(pi) from PurchaseItem pi where pi.product.id = :id")
    long purchaseItemCount(Long id);

    @Query("select count(si) from SaleItem si where si.product.id = :id")
    long saleItemCount(Long id);

    default boolean isUsed(Long id) {
        return movementCount(id) > 0 || purchaseItemCount(id) > 0 || saleItemCount(id) > 0;
    }
}
