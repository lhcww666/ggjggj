package com.example.integrations.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class DocumentTextExtractor {

    public String extract(MultipartFile file) throws Exception {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (name.endsWith(".txt") || name.endsWith(".md")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        if (name.endsWith(".docx")) {
            return extractDocx(file);
        }
        if (name.endsWith(".pdf")) {
            return extractPdf(file);
        }
        throw new IllegalArgumentException("演示版支持 txt、md、docx、pdf 文件");
    }

    private String extractDocx(MultipartFile file) throws Exception {
        StringBuilder result = new StringBuilder();
        try (InputStream input = file.getInputStream();
             XWPFDocument document = new XWPFDocument(input)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    result.append(text.trim()).append("\n");
                }
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            result.append(text.trim()).append(" ");
                        }
                    }
                    result.append("\n");
                }
            }
        }
        return result.toString().trim();
    }

    private String extractPdf(MultipartFile file) throws Exception {
        try (InputStream input = file.getInputStream();
             PDDocument document = PDDocument.load(input)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        }
    }
}
