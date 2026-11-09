package com.legaldocs.repository;

import com.legaldocs.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findTop10ByOrderBySentAtDesc();
    boolean existsByContractIdAndAlertTypeAndStatus(Long contractId, String alertType, String status);
    boolean existsByObligationIdAndAlertTypeAndStatus(Long obligationId, String alertType, String status);
}
