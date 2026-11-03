package com.villagemart.sales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, Long> {

    List<SalesReturn> findBySaleIdOrderByIdAsc(Long saleId);
}
