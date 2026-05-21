package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeOcrService {

    private final KnowledgeIntelligenceProperties properties;

    public String recognize(Path inputFile) {
        if (!properties.getOcr().isEnabled() || properties.getOcr().getCommand() == null || properties.getOcr().getCommand().isBlank()) {
            return "";
        }
        try {
            Path output = Files.createTempFile("knowledge-ocr-", ".txt");
            List<String> command = new ArrayList<>();
            command.add(properties.getOcr().getCommand());
            command.add(inputFile.toString());
            command.add(output.toString());
            command.add(properties.getOcr().getLanguage());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(Duration.ofSeconds(properties.getOcr().getTimeoutSeconds()).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "";
            }
            if (process.exitValue() != 0) {
                log.warn("OCR 命令执行失败，exitCode: {}", process.exitValue());
                return "";
            }
            return Files.exists(output) ? Files.readString(output, StandardCharsets.UTF_8) : "";
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OCR 命令调用失败: {}", e.getMessage());
            return "";
        }
    }
}
