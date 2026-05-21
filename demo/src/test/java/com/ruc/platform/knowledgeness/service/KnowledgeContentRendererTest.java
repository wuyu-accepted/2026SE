package com.ruc.platform.knowledgeness.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeContentRendererTest {

    private final KnowledgeContentRenderer renderer = new KnowledgeContentRenderer();

    @Test
    void markdownPreviewConvertsFileImagesToDownloadUrl() {
        String html = renderer.render("markdown", "# 标题\n\n![流程图](file:9201)");

        assertThat(html).contains("<h1>标题</h1>");
        assertThat(html).contains("<img src=\"/api/files/9201/download\" alt=\"流程图\" />");
    }

    @Test
    void latexPreviewConvertsFileImagesToDownloadUrlAndKeepsSourceEditable() {
        String html = renderer.render("latex", "\\section{请假流程}\n\\textbf{第一步}\n\\includegraphics{file:9202}");

        assertThat(html).contains("<h1>请假流程</h1>");
        assertThat(html).contains("<strong>第一步</strong>");
        assertThat(html).contains("<img src=\"/api/files/9202/download\" alt=\"latex image\" />");
        assertThat(renderer.sourceExtension("latex")).isEqualTo("tex");
        assertThat(renderer.sourceExtension("markdown")).isEqualTo("md");
    }
}
