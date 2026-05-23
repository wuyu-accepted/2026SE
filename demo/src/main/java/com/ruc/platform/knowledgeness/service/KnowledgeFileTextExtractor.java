package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeFileTextExtractor {

    private static final int MAX_TEXT_LENGTH = 120_000;
    private static final int MAX_ZIP_ENTRIES = 50;
    private static final int PDF_OCR_DPI = 180;

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
        if (filename.endsWith(".xlsx") || mimeType.contains("spreadsheetml")) {
            return extractWorkbook(path, true);
        }
        if (filename.endsWith(".xls") || mimeType.contains("vnd.ms-excel")) {
            return extractWorkbook(path, false);
        }
        if (filename.endsWith(".pptx") || mimeType.contains("presentationml")) {
            return extractPptx(path);
        }
        if (filename.endsWith(".ppt") || mimeType.contains("vnd.ms-powerpoint")) {
            return extractPpt(path);
        }
        if (filename.endsWith(".zip") || mimeType.contains("zip")) {
            return extractZip(path);
        }
        if (filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".markdown") || filename.endsWith(".csv") || mimeType.startsWith("text/")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        if (filename.endsWith(".html") || filename.endsWith(".htm") || mimeType.contains("html")) {
            return stripHtml(Files.readString(path, StandardCharsets.UTF_8));
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
        System.setProperty("java.awt.headless", "true");
        try (PDDocument document = PDDocument.load(path.toFile())) {
            String embeddedText = new PDFTextStripper().getText(document);
            if (hasMeaningfulText(embeddedText) || !ocrService.isAvailable()) {
                return embeddedText;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                Path imagePath = Files.createTempFile("knowledge-pdf-page-", ".png");
                try {
                    BufferedImage image = renderer.renderImageWithDPI(i, PDF_OCR_DPI, ImageType.RGB);
                    ImageIO.write(image, "png", imagePath.toFile());
                    String pageText = ocrService.recognize(imagePath);
                    if (pageText != null && !pageText.isBlank()) {
                        builder.append(pageText).append('\n');
                    }
                } finally {
                    Files.deleteIfExists(imagePath);
                }
            }
            return builder.toString();
        }
    }

    private boolean hasMeaningfulText(String text) {
        return text != null && text.replaceAll("\\s+", "").length() >= 10;
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

    private String extractWorkbook(Path path, boolean xlsx) throws IOException {
        try (InputStream input = Files.newInputStream(path); Workbook workbook = xlsx ? new XSSFWorkbook(input) : new HSSFWorkbook(input)) {
            DataFormatter formatter = new DataFormatter();
            StringBuilder builder = new StringBuilder();
            for (Sheet sheet : workbook) {
                builder.append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    row.forEach(cell -> builder.append(formatter.formatCellValue(cell)).append(' '));
                    builder.append('\n');
                }
            }
            return builder.toString();
        }
    }

    private String extractPptx(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); XMLSlideShow slideShow = new XMLSlideShow(input)) {
            StringBuilder builder = new StringBuilder();
            slideShow.getSlides().forEach(slide -> {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        builder.append(textShape.getText()).append('\n');
                    }
                }
            });
            return builder.toString();
        }
    }

    private String extractPpt(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); HSLFSlideShow slideShow = new HSLFSlideShow(input)) {
            StringBuilder builder = new StringBuilder();
            slideShow.getSlides().forEach(slide -> {
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        builder.append(textShape.getText()).append('\n');
                    }
                }
            });
            return builder.toString();
        }
    }

    private String extractZip(Path path) throws IOException {
        return extractZip(path, 0);
    }

    private String extractZip(Path path, int depth) throws IOException {
        if (depth > 2) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zipInput.getNextEntry()) != null && count < MAX_ZIP_ENTRIES) {
                if (entry.isDirectory()) {
                    continue;
                }
                count++;
                String name = Path.of(entry.getName()).getFileName().toString();
                String lowerName = name.toLowerCase(Locale.ROOT);
                Path temp = Files.createTempFile("knowledge-zip-entry-", suffixOf(lowerName));
                try {
                    Files.write(temp, zipInput.readNBytes(MAX_TEXT_LENGTH));
                    builder.append(name).append('\n').append(extractZipEntry(temp, lowerName, depth)).append('\n');
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        }
        return builder.toString();
    }

    private String extractZipEntry(Path path, String lowerName, int depth) throws IOException {
        if (lowerName.endsWith(".zip")) {
            return extractZip(path, depth + 1);
        }
        if (lowerName.endsWith(".pdf")) {
            return extractPdf(path);
        }
        if (lowerName.endsWith(".docx")) {
            return extractDocx(path);
        }
        if (lowerName.endsWith(".doc")) {
            return extractDoc(path);
        }
        if (lowerName.endsWith(".xlsx")) {
            return extractWorkbook(path, true);
        }
        if (lowerName.endsWith(".xls")) {
            return extractWorkbook(path, false);
        }
        if (lowerName.endsWith(".pptx")) {
            return extractPptx(path);
        }
        if (lowerName.endsWith(".ppt")) {
            return extractPpt(path);
        }
        if (isImage(lowerName, "")) {
            return ocrService.recognize(path);
        }
        if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            return stripHtml(Files.readString(path, StandardCharsets.UTF_8));
        }
        if (lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".csv")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        return "";
    }

    private String suffixOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return ".bin";
        }
        return filename.substring(index);
    }

    private String stripHtmlIfNeeded(String filename, String text) {
        if (filename.endsWith(".html") || filename.endsWith(".htm")) {
            return stripHtml(text);
        }
        return text;
    }

    private String stripHtml(String html) {
        return html.replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("<[^>]+>", " ");
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
