package com.legaldocs.controller;

import com.legaldocs.dto.ContractRequest;
import com.legaldocs.entity.*;
import com.legaldocs.repository.*;
import com.legaldocs.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractTypeRepository contractTypeRepository;
    private final UserRepository userRepository;
    private final ObligationRepository obligationRepository;

    @GetMapping
    public List<Contract> getAll(@RequestParam(required = false) String status) {
        return contractService.findAll(status);
    }

    @GetMapping("/{id}")
    public Contract getById(@PathVariable Long id) {
        return contractService.findById(id);
    }

    @PostMapping
    public Contract create(@Valid @RequestBody ContractRequest request) {
        return contractService.create(request);
    }

    @PutMapping("/{id}")
    public Contract update(@PathVariable Long id, @Valid @RequestBody ContractRequest request) {
        return contractService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Contrato y vectores eliminados correctamente"));
    }

    @PostMapping(value = "/{id}/upload-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Contract uploadPdf(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return contractService.uploadAndProcessPdf(id, file);
    }

    @GetMapping("/types")
    public List<ContractType> getContractTypes() {
        return contractTypeRepository.findAll();
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @PatchMapping("/obligations/{obligationId}/status")
    public Obligation updateObligationStatus(@PathVariable Long obligationId, @RequestParam String status) {
        Obligation ob = obligationRepository.findById(obligationId)
                .orElseThrow(() -> new RuntimeException("Obligación no encontrada"));
        ob.setStatus(status.toUpperCase());
        return obligationRepository.save(ob);
    }
}
