package com.legaldocs.ai;

import com.legaldocs.dto.RagQueryRequest;
import com.legaldocs.dto.RagQueryResponse;
import com.legaldocs.dto.RagQueryResponse.SourceReference;
import com.legaldocs.vector.QdrantContractVectorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ContractRagService {

    private final QdrantContractVectorService vectorService;
    private final ChatClient chatClient;

    public ContractRagService(QdrantContractVectorService vectorService, ChatClient.Builder chatClientBuilder) {
        this.vectorService = vectorService;
        this.chatClient = chatClientBuilder.build();
    }

    public RagQueryResponse askQuestion(RagQueryRequest request) {
        List<Document> retrievedDocs = vectorService.searchSimilarFragments(
                request.question(),
                request.contractId(),
                request.clauseCategory(),
                6
        );

        if (retrievedDocs.isEmpty()) {
            return new RagQueryResponse(
                "No se encontró información suficiente en los contratos registrados para responder a esta consulta con respaldo documental.",
                false,
                List.of()
            );
        }

        StringBuilder contextBuilder = new StringBuilder();
        List<SourceReference> references = new ArrayList<>();

        int index = 1;
        for (Document doc : retrievedDocs) {
            Map<String, Object> meta = doc.getMetadata();

            Long contractId = toLong(meta.get("contract_id"));
            String contractCode = String.valueOf(meta.getOrDefault("contract_code", "N/A"));
            String contractTitle = String.valueOf(meta.getOrDefault("contract_title", "Contrato"));
            String fileName = String.valueOf(meta.getOrDefault("file_name", "documento.pdf"));
            Integer pageNumber = toInt(meta.get("page_number"), 1);
            String sourceType = String.valueOf(meta.getOrDefault("source_type", "CHUNK"));
            String clauseTitle = String.valueOf(meta.getOrDefault("clause_title", "Fragmento"));
            String clauseCategory = String.valueOf(meta.getOrDefault("clause_category", "GENERAL"));

            contextBuilder.append(String.format(
                "[Fuente #%d | Contrato: %s (%s) | Archivo: %s | Página: %d | Cláusula/Sección: %s]\n%s\n\n",
                index, contractCode, contractTitle, fileName, pageNumber, clauseTitle, doc.getContent()
            ));

            references.add(new SourceReference(
                doc.getId(),
                contractId,
                contractCode,
                contractTitle,
                fileName,
                pageNumber,
                sourceType,
                clauseTitle,
                clauseCategory,
                doc.getContent()
            ));
            index++;
        }

        String systemPrompt = """
                Eres el sistema de consulta documental inteligente de LegalDocs.
                REGLAS ESTRICTAS:
                1. Responde ÚNICAMENTE utilizando la información contenida en los FRAGMENTOS RECUPERADOS.
                2. Si los fragmentos no contienen la respuesta a la pregunta del usuario, indica explícitamente que no hay información suficiente en el contrato. NUNCA inventes cláusulas, montos ni fechas.
                3. Cita siempre la fuente entre corchetes indicando el código de contrato y la página, por ejemplo: [Contrato CNT-2026-001, Pág. 3].
                4. No emitas conceptos ni dictámenes jurídicos definitivos; limítate a exponer de forma clara y estructurada lo que dice el texto contractual.
                """;

        String userPrompt = "FRAGMENTOS RECUPERADOS DE QDRANT:\n" + contextBuilder +
                            "\nPREGUNTA DEL USUARIO:\n" + request.question();

        String aiAnswer = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        return new RagQueryResponse(aiAnswer, true, references);
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(val)); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object val, int defaultVal) {
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return defaultVal; }
    }
}
