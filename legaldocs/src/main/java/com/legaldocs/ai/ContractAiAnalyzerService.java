package com.legaldocs.ai;

import com.legaldocs.dto.AiExtractionResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ContractAiAnalyzerService {

    private final ChatClient chatClient;

    public ContractAiAnalyzerService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public AiExtractionResult analyzeContractText(String annotatedText) {
        String truncatedText = annotatedText.length() > 35000
                ? annotatedText.substring(0, 35000)
                : annotatedText;

        String systemPrompt = """
                Eres un asistente experto en organización documental de contratos corporativos.
                Analiza el texto del contrato adjunto (que incluye marcadores de [PÁGINA X]) y extrae:
                1. suggestedContractType: Una de ["Prestación de Servicios", "Confidencialidad (NDA)", "Arrendamiento", "Suministro / Proveedores", "Laboral"].
                2. executiveSummary: Resumen objetivo y claro del contrato en máximo 4 líneas, sin emitir juicios jurídicos definitivos.
                3. clauses: Lista de las cláusulas más importantes identificando su categoría exacta (CONFIDENTIALITY, PENALTY, TERMINATION, PAYMENT, OBLIGATION, OTHER), título, extracto literal y el número de página donde aparece.
                4. obligations: Lista de obligaciones concretas o hitos a cumplir, indicando descripción y fecha límite en formato YYYY-MM-DD si aparece en el texto.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user("Texto del contrato:\n" + truncatedText)
                .call()
                .entity(AiExtractionResult.class);
    }
}
