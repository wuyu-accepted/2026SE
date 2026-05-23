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
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeOcrService {

    private final KnowledgeIntelligenceProperties properties;

    public boolean isAvailable() {
        return properties.getOcr().isEnabled()
                && properties.getOcr().getCommand() != null
                && !properties.getOcr().getCommand().isBlank();
    }

    public String recognize(Path inputFile) {
        if (!isAvailable()) {
            return "";
        }
        Path output = null;
        try {
            output = Files.createTempFile("knowledge-ocr-", "");
            List<String> command = new ArrayList<>();
            command.add(properties.getOcr().getCommand());
            command.add(inputFile.toString());
            command.add(output.toString());
            command.add("-l");
            command.add(properties.getOcr().getLanguage());
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(Duration.ofSeconds(properties.getOcr().getTimeoutSeconds()).toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("OCR 命令超时，file: {}", inputFile);
                return "";
            }
            if (process.exitValue() != 0) {
                String outputText = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("OCR 命令执行失败，exitCode: {}, output: {}", process.exitValue(), outputText);
                return "";
            }
            Path txtPath = Path.of(output + ".txt");
            if (Files.exists(txtPath)) {
                return Files.readString(txtPath, StandardCharsets.UTF_8);
            }
            return Files.exists(output) ? Files.readString(output, StandardCharsets.UTF_8) : "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("OCR 命令调用被中断: {}", e.getMessage());
            return "";
        } catch (IOException e) {
            log.warn("OCR 命令调用失败: {}", e.getMessage());
            return "";
        } finally {
            if (output != null) {
                try {
                    Files.deleteIfExists(output);
                    Files.deleteIfExists(Path.of(output + ".txt"));
                } catch (IOException ignored) {
                }
            }
        }
    }
}
