package com.legaldocs.repository;

import com.legaldocs.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByContractIdOrderByVersionDesc(Long contractId);
}
