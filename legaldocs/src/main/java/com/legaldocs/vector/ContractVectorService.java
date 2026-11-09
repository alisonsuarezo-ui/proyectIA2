package com.legaldocs.vector;

import com.legaldocs.ai.PdfExtractorService.PageContent;
import com.legaldocs.entity.Clause;
import com.legaldocs.entity.Contract;
import com.legaldocs.entity.Document;
import java.util.List;

public interface ContractVectorService {
    void indexContractDocument(Contract contract, Document document, List<PageContent> pages, List<Clause> savedClauses);
    void deleteContractVectors(Long contractId);
}
