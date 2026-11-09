package com.legaldocs.vector;

import com.legaldocs.ai.PdfExtractorService.PageContent;
import com.legaldocs.entity.Clause;
import com.legaldocs.entity.Contract;
import com.legaldocs.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document as AiDocument;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantContractVectorService implements ContractVectorService {

    private final VectorStore vectorStore;

    @Override
    public void indexContractDocument(Contract contract, Document doc, List<PageContent> pages, List<Clause> savedClauses) {
        List<AiDocument> vectorDocuments = new ArrayList<>();

        TokenTextSplitter textSplitter = new TokenTextSplitter(400, 100, 10, 5000, true);

        for (PageContent page : pages) {
            Map<String, Object> baseMeta = new HashMap<>();
            baseMeta.put("contract_id", contract.getId());
            baseMeta.put("contract_code", contract.getCode());
            baseMeta.put("contract_title", contract.getTitle());
            baseMeta.put("document_id", doc.getId());
            baseMeta.put("file_name", doc.getFileName());
            baseMeta.put("page_number", page.pageNumber());
            baseMeta.put("source_type", "CHUNK");
            baseMeta.put("clause_title", "Texto General");
            baseMeta.put("clause_category", "GENERAL");

            AiDocument pageDoc = new AiDocument(page.text(), baseMeta);
            List<AiDocument> chunks = textSplitter.apply(List.of(pageDoc));
            vectorDocuments.addAll(chunks);
        }

        for (Clause clause : savedClauses) {
            Map<String, Object> clauseMeta = new HashMap<>();
            clauseMeta.put("contract_id", contract.getId());
            clauseMeta.put("contract_code", contract.getCode());
            clauseMeta.put("contract_title", contract.getTitle());
            clauseMeta.put("document_id", doc.getId());
            clauseMeta.put("file_name", doc.getFileName());
            clauseMeta.put("page_number", clause.getPageNumber() != null ? clause.getPageNumber() : 1);
            clauseMeta.put("source_type", "CLAUSE");
            clauseMeta.put("clause_id", clause.getId());
            clauseMeta.put("clause_title", clause.getTitle());
            clauseMeta.put("clause_category", clause.getCategory());

            String enrichedContent = "CLÁUSULA [" + clause.getCategory() + " - " + clause.getTitle() + "]: " + clause.getContent();
            AiDocument clauseVectorDoc = new AiDocument(clause.getQdrantPointId(), enrichedContent, clauseMeta);
            vectorDocuments.add(clauseVectorDoc);
        }

        if (!vectorDocuments.isEmpty()) {
            vectorStore.add(vectorDocuments);
            log.info("Indexados {} vectores en Qdrant para el contrato {}", vectorDocuments.size(), contract.getCode());
        }
    }

    @Override
    public void deleteContractVectors(Long contractId) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            SearchRequest request = SearchRequest.query("contrato")
                    .withTopK(200)
                    .withSimilarityThreshold(0.0)
                    .withFilterExpression(b.eq("contract_id", contractId).build());

            List<AiDocument> existingDocs = vectorStore.similaritySearch(request);
            if (!existingDocs.isEmpty()) {
                List<String> idsToDelete = existingDocs.stream().map(AiDocument::getId).toList();
                vectorStore.delete(idsToDelete);
                log.info("Eliminados {} vectores de Qdrant asociados al contract_id={}", idsToDelete.size(), contractId);
            }
        } catch (Exception e) {
            log.warn("Aviso al eliminar vectores en Qdrant para contract_id={}: {}", contractId, e.getMessage());
        }
    }

    public List<AiDocument> searchSimilarFragments(String query, Long contractId, String clauseCategory, int topK) {
        SearchRequest.Builder searchBuilder = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(0.55);

        FilterExpressionBuilder b = new FilterExpressionBuilder();

        if (contractId != null && clauseCategory != null && !clauseCategory.isBlank()) {
            searchBuilder.withFilterExpression(
                b.and(
                    b.eq("contract_id", contractId),
                    b.eq("clause_category", clauseCategory.toUpperCase())
                ).build()
            );
        } else if (contractId != null) {
            searchBuilder.withFilterExpression(b.eq("contract_id", contractId).build());
        } else if (clauseCategory != null && !clauseCategory.isBlank()) {
            searchBuilder.withFilterExpression(b.eq("clause_category", clauseCategory.toUpperCase()).build());
        }

        return vectorStore.similaritySearch(searchBuilder.build());
    }
}
