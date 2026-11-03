package com.villagemart.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("""
            select s from Supplier s
            where (:q is null or lower(s.name) like :q or s.mobile like :q)
              and (:active is null or s.active = :active)
            """)
    Page<Supplier> search(String q, Boolean active, Pageable pageable);
}
