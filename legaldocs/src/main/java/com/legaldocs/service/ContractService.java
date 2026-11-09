package com.legaldocs.service;

import com.legaldocs.ai.ContractAiAnalyzerService;
import com.legaldocs.ai.PdfExtractorService;
import com.legaldocs.ai.PdfExtractorService.PdfDocumentData;
import com.legaldocs.dto.AiExtractionResult;
import com.legaldocs.dto.ContractRequest;
import com.legaldocs.entity.*;
import com.legaldocs.exception.BusinessException;
import com.legaldocs.repository.*;
import com.legaldocs.vector.ContractVectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractTypeRepository contractTypeRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ClauseRepository clauseRepository;
    private final ObligationRepository obligationRepository;
    private final PdfExtractorService pdfExtractorService;
    private final ContractAiAnalyzerService aiAnalyzerService;
    private final ContractVectorService contractVectorService;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public List<Contract> findAll(String statusFilter) {
        refreshContractStatuses();
        if (statusFilter != null && !statusFilter.isBlank()) {
            return contractRepository.findByStatus(statusFilter.toUpperCase());
        }
        return contractRepository.findAll();
    }

    public Contract findById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Contrato no encontrado con ID: " + id));
    }

    @Transactional
    public Contract create(ContractRequest req) {
        if (contractRepository.findByCode(req.code()).isPresent()) {
            throw new BusinessException("Ya existe un contrato con el código: " + req.code());
        }

        Contract contract = Contract.builder()
                .code(req.code())
                .title(req.title())
                .counterparty(req.counterparty())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .observations(req.observations())
                .status(calculateStatus(req.endDate()))
                .build();

        assignRelations(contract, req.contractTypeId(), req.responsibleUserId());
        return contractRepository.save(contract);
    }

    @Transactional
    public Contract update(Long id, ContractRequest req) {
        Contract contract = findById(id);
        contract.setTitle(req.title());
        contract.setCounterparty(req.counterparty());
        contract.setStartDate(req.startDate());
        contract.setEndDate(req.endDate());
        contract.setObservations(req.observations());
        contract.setStatus(calculateStatus(req.endDate()));
        assignRelations(contract, req.contractTypeId(), req.responsibleUserId());
        return contractRepository.save(contract);
    }

    @Transactional
    public void delete(Long id) {
        Contract contract = findById(id);
        contractVectorService.deleteContractVectors(id);
        contractRepository.delete(contract);
    }

    @Transactional
    public Contract uploadAndProcessPdf(Long contractId, MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("Debes adjuntar un archivo PDF válido.");
        }

        Contract contract = findById(contractId);
        File savedFile = storePhysicalFile(contract.getCode(), file);
        PdfDocumentData pdfData = pdfExtractorService.extractText(savedFile);

        int nextVersion = documentRepository.findByContractIdOrderByVersionDesc(contractId)
                .stream().mapToInt(Document::getVersion).max().orElse(0) + 1;

        Document doc = Document.builder()
                .contract(contract)
                .fileName(file.getOriginalFilename())
                .filePath(savedFile.getAbsolutePath())
                .pageCount(pdfData.totalPages())
                .version(nextVersion)
                .processedByAi(true)
                .build();
        doc = documentRepository.save(doc);

        AiExtractionResult aiResult = aiAnalyzerService.analyzeContractText(pdfData.fullAnnotatedText());

        if (aiResult.executiveSummary() != null) {
            contract.setSummary(aiResult.executiveSummary());
        }

        if (contract.getContractType() == null && aiResult.suggestedContractType() != null) {
            contractTypeRepository.findByNameIgnoreCase(aiResult.suggestedContractType())
                    .ifPresent(contract::setContractType);
        }

        List<Clause> savedClauses = new ArrayList<>();
        if (aiResult.clauses() != null) {
            for (AiExtractionResult.ExtractedClause ec : aiResult.clauses()) {
                Clause clause = Clause.builder()
                        .contract(contract)
                        .title(ec.title() != null ? ec.title() : "Cláusula")
                        .category(ec.category() != null ? ec.category() : "OTHER")
                        .content(ec.content())
                        .pageNumber(ec.pageNumber() != null ? ec.pageNumber() : 1)
                        .qdrantPointId(UUID.randomUUID().toString())
                        .build();
                savedClauses.add(clauseRepository.save(clause));
            }
        }

        if (aiResult.obligations() != null) {
            for (AiExtractionResult.ExtractedObligation eo : aiResult.obligations()) {
                LocalDate dueDate = parseSafeDate(eo.dueDateIso(), contract.getEndDate());
                Obligation obligation = Obligation.builder()
                        .contract(contract)
                        .description(eo.description())
                        .dueDate(dueDate)
                        .status("PENDING")
                        .responsibleEmail(contract.getResponsibleUser() != null
                                ? contract.getResponsibleUser().getEmail()
                                : "admin@legaldocs.com")
                        .build();
                obligationRepository.save(obligation);
            }
        }

        contractVectorService.indexContractDocument(contract, doc, pdfData.pages(), savedClauses);
        return contractRepository.save(contract);
    }

    @Transactional
    public void refreshContractStatuses() {
        for (Contract c : contractRepository.findAll()) {
            if (!"TERMINATED".equals(c.getStatus())) {
                String newStatus = calculateStatus(c.getEndDate());
                if (!newStatus.equals(c.getStatus())) {
                    c.setStatus(newStatus);
                    contractRepository.save(c);
                }
            }
        }
    }

    private String calculateStatus(LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (endDate.isBefore(today)) return "EXPIRED";
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, endDate);
        if (daysUntilExpiry <= 30) return "EXPIRING_SOON";
        return "ACTIVE";
    }

    private void assignRelations(Contract contract, Long typeId, Long userId) {
        if (typeId != null) {
            contractTypeRepository.findById(typeId).ifPresent(contract::setContractType);
        }
        if (userId != null) {
            userRepository.findById(userId).ifPresent(contract::setResponsibleUser);
        } else {
            userRepository.findAll().stream().findFirst().ifPresent(contract::setResponsibleUser);
        }
    }

    private File storePhysicalFile(String contractCode, MultipartFile file) {
        try {
            Path dirPath = Paths.get(uploadDir);
            Files.createDirectories(dirPath);
            String cleanName = contractCode + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = dirPath.resolve(cleanName);
            file.transferTo(targetPath);
            return targetPath.toFile();
        } catch (IOException e) {
            throw new BusinessException("No se pudo almacenar el PDF en disco: " + e.getMessage());
        }
    }

    private LocalDate parseSafeDate(String isoDate, LocalDate fallback) {
        try {
            if (isoDate != null && !isoDate.isBlank()) {
                return LocalDate.parse(isoDate.trim());
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}
