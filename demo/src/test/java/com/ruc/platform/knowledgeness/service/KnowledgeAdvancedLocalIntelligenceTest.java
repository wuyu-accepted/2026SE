package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import com.ruc.platform.knowledgeness.entity.KnowledgeArticle;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeAdvancedLocalIntelligenceTest {

    @TempDir
    Path tempDir;

    @Test
    void semanticSearchShouldUseLuceneVectorIndex() {
        KnowledgeIntelligenceProperties properties = new KnowledgeIntelligenceProperties();
        properties.getSemantic().setIndexPath(tempDir.resolve("vec-file").toString());
        properties.getSemantic().setVectorIndexPath(tempDir.resolve("vec-hnsw").toString());
        properties.getSemantic().setMinScore(0.01);
        KnowledgeSynonymService synonymService = new KnowledgeSynonymService(null, properties);
        KnowledgeEmbeddingModel model = text -> text.contains("资助") || text.contains("困难") || text.contains("助学金")
                ? new double[]{1D, 0D, 0D}
                : new double[]{0D, 1D, 0D};
        KnowledgeSemanticSearchService service = new KnowledgeSemanticSearchService(properties, synonymService, model);
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(9001L);
        article.setTitle("困难学生资助说明");
        article.setExtractedText("助学金和绿色通道材料");

        service.upsertArticle(article);
        List<KnowledgeSemanticSearchService.SemanticHit> hits = service.search("资助怎么办", 5);

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getArticleId()).isEqualTo(9001L);
        assertThat(hits.get(0).getReason()).contains("HNSW");
    }

    @Test
    void zipExtractorShouldParseNestedOfficeFiles() throws Exception {
        Path docx = tempDir.resolve("policy.docx");
        try (XWPFDocument document = new XWPFDocument(); OutputStream output = Files.newOutputStream(docx)) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("压缩包内请假流程和销假材料");
            document.write(output);
        }
        Path zip = tempDir.resolve("files.zip");
        try (ZipOutputStream zipOutput = new ZipOutputStream(Files.newOutputStream(zip))) {
            zipOutput.putNextEntry(new ZipEntry("docs/policy.docx"));
            zipOutput.write(Files.readAllBytes(docx));
            zipOutput.closeEntry();
        }
        FileService fileService = mock(FileService.class);
        FileMetadata metadata = new FileMetadata();
        metadata.setId(7001L);
        metadata.setOriginName("files.zip");
        metadata.setStoragePath(zip.toString());
        metadata.setMimeType("application/zip");
        when(fileService.getFileMetadata(7001L)).thenReturn(metadata);
        KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor(fileService, mock(KnowledgeOcrService.class));

        String text = extractor.extract(7001L);

        assertThat(text).contains("请假流程").contains("销假材料");
    }
}
