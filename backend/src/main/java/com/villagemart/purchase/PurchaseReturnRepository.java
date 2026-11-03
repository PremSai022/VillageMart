package com.villagemart.purchase;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

    List<PurchaseReturn> findByPurchaseIdOrderByIdAsc(Long purchaseId);
}
