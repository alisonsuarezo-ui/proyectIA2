package com.legaldocs.repository;

import com.legaldocs.entity.Obligation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ObligationRepository extends JpaRepository<Obligation, Long> {
    List<Obligation> findByContractId(Long contractId);
    List<Obligation> findByStatusOrderByDueDateAsc(String status);
    long countByStatus(String status);
    List<Obligation> findByStatusAndDueDateBetween(String status, LocalDate start, LocalDate end);
}
