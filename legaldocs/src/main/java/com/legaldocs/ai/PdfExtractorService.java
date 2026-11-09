package com.legaldocs.ai;

import com.legaldocs.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractorService {

    public record PageContent(int pageNumber, String text) {}
    public record PdfDocumentData(int totalPages, String fullAnnotatedText, List<PageContent> pages) {}

    public PdfDocumentData extractText(File pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageContent> pages = new ArrayList<>();
            StringBuilder fullText = new StringBuilder();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (!pageText.isBlank()) {
                    pages.add(new PageContent(page, pageText));
                    fullText.append("\n--- [PÁGINA ").append(page).append("] ---\n").append(pageText);
                }
            }

            if (pages.isEmpty()) {
                throw new BusinessException("El archivo PDF no contiene texto extraíble.");
            }

            return new PdfDocumentData(totalPages, fullText.toString(), pages);
        } catch (IOException e) {
            throw new BusinessException("Error al leer el archivo PDF: " + e.getMessage());
        }
    }
}
