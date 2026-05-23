package com.ruc.platform.knowledgeness.service;

import com.ruc.platform.knowledgeness.config.KnowledgeIntelligenceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeLatexCompileService {

    private final KnowledgeIntelligenceProperties properties;

    public byte[] compileToPdf(String source) {
        if (!properties.getLatex().isEnabled() || source == null || source.isBlank()) {
            return new byte[0];
        }
        try {
            Path dir = Files.createTempDirectory("knowledge-latex-");
            Path tex = dir.resolve("main.tex");
            Files.writeString(tex, source, StandardCharsets.UTF_8);
            Process process = new ProcessBuilder(properties.getLatex().getCommand(), tex.toString())
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(Duration.ofSeconds(properties.getLatex().getTimeoutSeconds()).toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new byte[0];
            }
            Path pdf = dir.resolve("main.pdf");
            return Files.exists(pdf) ? Files.readAllBytes(pdf) : new byte[0];
        } catch (Exception e) {
            log.warn("LaTeX 本地编译失败: {}", e.getMessage());
            return new byte[0];
        }
    }
}
