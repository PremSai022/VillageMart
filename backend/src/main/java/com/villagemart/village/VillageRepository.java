package com.villagemart.village;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VillageRepository extends JpaRepository<Village, Long> {

    @Query("""
            select v from Village v
            where (:q is null or lower(v.name) like :q or lower(v.district) like :q)
              and (:active is null or v.active = :active)
            """)
    Page<Village> search(String q, Boolean active, Pageable pageable);

    @Query("""
            select v from Village v
            where lower(v.name) = lower(:name)
              and ((:district is null and v.district is null) or lower(v.district) = lower(:district))
            """)
    List<Village> findSame(String name, String district);
}
