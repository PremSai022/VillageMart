package com.villagemart.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query(value = """
            select c from Customer c left join fetch c.village v
            where (:q is null or lower(c.name) like :q or c.mobile like :q)
              and (:villageId is null or v.id = :villageId)
              and (:active is null or c.active = :active)
            """,
            countQuery = """
            select count(c) from Customer c
            where (:q is null or lower(c.name) like :q or c.mobile like :q)
              and (:villageId is null or c.village.id = :villageId)
              and (:active is null or c.active = :active)
            """)
    Page<Customer> search(String q, Long villageId, Boolean active, Pageable pageable);

    Optional<Customer> findByMobile(String mobile);

    @Query("select c from Customer c left join fetch c.village where c.id = :id")
    Optional<Customer> findDetailed(Long id);
}
