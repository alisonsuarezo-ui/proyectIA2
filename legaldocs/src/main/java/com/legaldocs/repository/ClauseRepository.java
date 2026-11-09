package com.legaldocs.repository;

import com.legaldocs.entity.Clause;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClauseRepository extends JpaRepository<Clause, Long> {
    List<Clause> findByContractId(Long contractId);
}
