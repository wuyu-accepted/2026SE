package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeFileTextExtractor {

    private static final int MAX_TEXT_LENGTH = 120_000;

    private final FileService fileService;
    private final KnowledgeOcrService ocrService;

    public String extract(Long fileId) {
        if (fileId == null) {
            return "";
        }
        FileMetadata metadata = fileService.getFileMetadata(fileId);
        try {
            return limit(normalize(extract(metadata)));
        } catch (IOException e) {
            log.warn("知识库文件文字抽取失败，fileId: {}, fileName: {}", fileId, metadata.getOriginName(), e);
            return "";
        }
    }

    private String extract(FileMetadata metadata) throws IOException {
        String filename = metadata.getOriginName() == null ? "" : metadata.getOriginName().toLowerCase(Locale.ROOT);
        String mimeType = metadata.getMimeType() == null ? "" : metadata.getMimeType().toLowerCase(Locale.ROOT);
        Path path = Path.of(metadata.getStoragePath());
        if (filename.endsWith(".pdf") || mimeType.contains("pdf")) {
            return extractPdf(path);
        }
        if (filename.endsWith(".docx") || mimeType.contains("wordprocessingml")) {
            return extractDocx(path);
        }
        if (filename.endsWith(".doc") || mimeType.equals("application/msword")) {
            return extractDoc(path);
        }
        if (filename.endsWith(".txt") || mimeType.startsWith("text/")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        if (isImage(filename, mimeType)) {
            return ocrService.recognize(path);
        }
        return "";
    }


    private boolean isImage(String filename, String mimeType) {
        return mimeType.startsWith("image/")
                || filename.endsWith(".png")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".bmp")
                || filename.endsWith(".tif")
                || filename.endsWith(".tiff");
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(input)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private String extractDoc(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); HWPFDocument document = new HWPFDocument(input); WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String limit(String text) {
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TEXT_LENGTH);
    }
}
