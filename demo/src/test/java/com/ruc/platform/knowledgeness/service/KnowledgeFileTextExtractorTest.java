package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeFileTextExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsTextFileContent() throws Exception {
        Path file = tempDir.resolve("policy.txt");
        Files.writeString(file, "奖助学金\n家庭经济困难认定");
        FileService fileService = mock(FileService.class);
        when(fileService.getFileMetadata(1L)).thenReturn(metadata(file, "policy.txt", "text/plain"));
        KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor(fileService, mock(KnowledgeOcrService.class));

        String text = extractor.extract(1L);

        assertThat(text).contains("奖助学金").contains("家庭经济困难认定");
    }

    @Test
    void extractsDocxParagraphContent() throws Exception {
        Path file = tempDir.resolve("process.docx");
        try (XWPFDocument document = new XWPFDocument(); OutputStream output = Files.newOutputStream(file)) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("请假流程 返校销假 审批材料");
            document.write(output);
        }
        FileService fileService = mock(FileService.class);
        when(fileService.getFileMetadata(2L)).thenReturn(metadata(file, "process.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor(fileService, mock(KnowledgeOcrService.class));

        String text = extractor.extract(2L);

        assertThat(text).contains("请假流程").contains("返校销假");
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
