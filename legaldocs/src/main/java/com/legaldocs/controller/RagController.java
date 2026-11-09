package com.legaldocs.controller;

import com.legaldocs.ai.ContractRagService;
import com.legaldocs.dto.RagQueryRequest;
import com.legaldocs.dto.RagQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final ContractRagService contractRagService;

    @PostMapping("/ask")
    public RagQueryResponse ask(@Valid @RequestBody RagQueryRequest request) {
        return contractRagService.askQuestion(request);
    }
}
