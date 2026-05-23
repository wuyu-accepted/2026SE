package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeIntelligenceEnhancementTest {

    @TempDir
    Path tempDir;

    @Test
    void imageUploadShouldUseLocalOcrText() throws Exception {
        Path image = tempDir.resolve("notice.png");
        Files.writeString(image, "fake image bytes");
        FileService fileService = mock(FileService.class);
        KnowledgeOcrService ocrService = mock(KnowledgeOcrService.class);
        when(fileService.getFileMetadata(11L)).thenReturn(metadata(image, "notice.png", "image/png"));
        when(ocrService.recognize(image)).thenReturn("扫描材料 绿色通道 办理说明");
        KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor(fileService, ocrService);

        String text = extractor.extract(11L);

        assertThat(text).contains("绿色通道");
    }

    @Test
    void scannedPdfShouldRenderPagesAndUseOcrWhenEmbeddedTextIsEmpty() throws Exception {
        Path image = tempDir.resolve("page.png");
        BufferedImage bufferedImage = new BufferedImage(100, 40, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < bufferedImage.getWidth(); x++) {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                bufferedImage.setRGB(x, y, 0xFFFFFF);
            }
        }
        ImageIO.write(bufferedImage, "png", image.toFile());
        Path pdf = tempDir.resolve("scanned.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDImageXObject pdImage = PDImageXObject.createFromFileByContent(image.toFile(), document);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                stream.drawImage(pdImage, 10, 700, 100, 40);
            }
            document.save(pdf.toFile());
        }
        FileService fileService = mock(FileService.class);
        KnowledgeOcrService ocrService = mock(KnowledgeOcrService.class);
        when(fileService.getFileMetadata(12L)).thenReturn(metadata(pdf, "scanned.pdf", "application/pdf"));
        when(ocrService.isAvailable()).thenReturn(true);
        when(ocrService.recognize(org.mockito.ArgumentMatchers.any(Path.class))).thenReturn("扫描 PDF 户籍证明 办理流程");
        KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor(fileService, ocrService);

        String text = extractor.extract(12L);

        assertThat(text).contains("户籍证明").contains("办理流程");
    }

    @Test
    void localSearchShouldHighlightAndExplainChineseNgramMatches() {
        KnowledgeIntelligenceProperties properties = new KnowledgeIntelligenceProperties();
        properties.getSearch().setIndexPath(tempDir.resolve("lucene").toString());
        KnowledgeLocalSearchService searchService = new KnowledgeLocalSearchService(properties);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(21L);
        article.setTitle("家庭经济困难认定指南");
        article.setSummary("奖助学金申请前置材料");
        article.setExtractedText("学生需要提交家庭情况说明和证明材料");

        searchService.indexArticle(article);
        List<KnowledgeLocalSearchService.SearchHit> hits = searchService.search("困难认定", 10);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getArticleId()).isEqualTo(21L);
        assertThat(hits.get(0).getHighlight()).contains("<mark>");
        assertThat(hits.get(0).getScoreExplanation()).contains("标题");
    }

    @Test
    void semanticSearchShouldReturnNearMeaningArticleWithoutSameKeyword() {
        KnowledgeIntelligenceProperties properties = new KnowledgeIntelligenceProperties();
        properties.getSemantic().setIndexPath(tempDir.resolve("vectors").toString());
        KnowledgeSemanticSearchService semanticSearchService = new KnowledgeSemanticSearchService(properties);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(31L);
        article.setTitle("奖助学金申请说明");
        article.setSummary("助学金和补助材料清单");
        article.setExtractedText("家庭经济情况核验后可申请补助");
        semanticSearchService.upsertArticle(article);

        List<Long> ids = semanticSearchService.searchArticleIds("困难资助怎么申请", 5);

        assertThat(ids).contains(31L);
    }

    private FileMetadata metadata(Path file, String name, String mimeType) throws Exception {
        FileMetadata metadata = new FileMetadata();
        metadata.setId(1L);
        metadata.setOriginName(name);
        metadata.setStoragePath(file.toString());
        metadata.setMimeType(mimeType);
        metadata.setFileSize(Files.size(file));
        return metadata;
    }
}
