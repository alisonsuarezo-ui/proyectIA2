package com.legaldocs.repository;

import com.legaldocs.entity.ContractType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContractTypeRepository extends JpaRepository<ContractType, Long> {
    Optional<ContractType> findByNameIgnoreCase(String name);
}
