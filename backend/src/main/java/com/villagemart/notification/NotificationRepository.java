package com.villagemart.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByProductIdAndAlertActiveTrue(Long productId);

    @Query(value = "select n from Notification n left join fetch n.product where (:unreadOnly = false or n.read = false)",
            countQuery = "select count(n) from Notification n where (:unreadOnly = false or n.read = false)")
    Page<Notification> search(boolean unreadOnly, Pageable pageable);

    long countByReadFalse();

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :now, n.updatedAt = :now where n.read = false")
    int markAllRead(Instant now);

    List<Notification> findTop50ByPushStatusAndPushAttemptsLessThanOrderByIdAsc(PushStatus status, int attempts);

    @Query("select n from Notification n left join fetch n.product where n.alertActive = true order by n.createdAt desc")
    List<Notification> findActiveAlerts();
}
