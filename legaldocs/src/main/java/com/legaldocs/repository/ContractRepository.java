package com.legaldocs.repository;

import com.legaldocs.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByCode(String code);
    List<Contract> findByStatus(String status);
    long countByStatus(String status);

    @Query("SELECT c FROM Contract c WHERE c.endDate BETWEEN :today AND :limitDate AND c.status <> 'TERMINATED'")
    List<Contract> findExpiringContracts(@Param("today") LocalDate today, @Param("limitDate") LocalDate limitDate);

    @Query("SELECT c FROM Contract c WHERE c.endDate < :today AND c.status <> 'EXPIRED' AND c.status <> 'TERMINATED'")
    List<Contract> findExpiredContracts(@Param("today") LocalDate today);
}
