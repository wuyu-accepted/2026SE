package com.ruc.platform.knowledgeness.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KnowledgeContentRenderer {

    public String render(String editorType, String source) {
        if (source == null || source.isBlank()) {
            return "";
        }
        if ("latex".equalsIgnoreCase(editorType)) {
            return renderLatex(source);
        }
        return renderMarkdown(source);
    }

    public String sourceExtension(String editorType) {
        return "latex".equalsIgnoreCase(editorType) ? "tex" : "md";
    }

    public String renderMarkdown(String source) {
        StringBuilder html = new StringBuilder();
        boolean inList = false;
        for (String rawLine : source.split("\\R", -1)) {
            String line = rawLine.stripTrailing();
            if (line.isBlank()) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                continue;
            }
            if (line.startsWith("# ")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<h1>").append(inlineMarkdown(line.substring(2))).append("</h1>");
            } else if (line.startsWith("## ")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<h2>").append(inlineMarkdown(line.substring(3))).append("</h2>");
            } else if (line.startsWith("### ")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<h3>").append(inlineMarkdown(line.substring(4))).append("</h3>");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(inlineMarkdown(line.substring(2))).append("</li>");
            } else {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                html.append("<p>").append(inlineMarkdown(line)).append("</p>");
            }
        }
        if (inList) {
            html.append("</ul>");
        }
        return html.toString();
    }

    public String renderLatex(String source) {
        String body = source
                .replaceAll("(?s)\\\\documentclass\\s*\\{[^}]+}", "")
                .replace("\\begin{document}", "")
                .replace("\\end{document}", "");
        StringBuilder html = new StringBuilder();
        for (String line : body.split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Matcher sectionMatcher = Pattern.compile("^\\\\section\\{([^}]+)}$").matcher(trimmed);
            Matcher subsectionMatcher = Pattern.compile("^\\\\subsection\\{([^}]+)}$").matcher(trimmed);
            Matcher imageMatcher = Pattern.compile("^\\\\includegraphics(?:\\[[^]]*])?\\{([^}]+)}$").matcher(trimmed);
            if (sectionMatcher.matches()) {
                html.append("<h1>").append(escapeHtml(sectionMatcher.group(1))).append("</h1>");
            } else if (subsectionMatcher.matches()) {
                html.append("<h2>").append(escapeHtml(subsectionMatcher.group(1))).append("</h2>");
            } else if (imageMatcher.matches()) {
                html.append(latexImage(imageMatcher.group(1)));
            } else {
                html.append("<p>").append(inlineLatex(trimmed)).append("</p>");
            }
        }
        return html.toString();
    }

    private String inlineMarkdown(String text) {
        String escaped = escapeHtml(text);
        escaped = replaceMarkdownImages(escaped);
        escaped = escaped.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        escaped = escaped.replaceAll("`([^`]+)`", "<code>$1</code>");
        return escaped;
    }

    private String inlineLatex(String text) {
        String rendered = escapeHtml(text);
        rendered = replaceLatexImages(rendered);
        rendered = rendered.replaceAll("\\\\textbf\\{([^}]+)}", "<strong>$1</strong>");
        rendered = rendered.replaceAll("\\\\emph\\{([^}]+)}", "<em>$1</em>");
        return rendered.replace("\\\\", "<br />");
    }

    private String replaceMarkdownImages(String text) {
        Pattern pattern = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String alt = matcher.group(1);
            String src = normalizeImageSource(matcher.group(2));
            matcher.appendReplacement(sb, Matcher.quoteReplacement("<img src=\"" + escapeHtml(src) + "\" alt=\"" + alt + "\" />"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String replaceLatexImages(String text) {
        Pattern pattern = Pattern.compile("\\\\includegraphics(?:\\[[^]]*])?\\{([^}]+)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(latexImage(matcher.group(1))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String latexImage(String src) {
        return "<img src=\"" + escapeHtml(normalizeImageSource(src)) + "\" alt=\"latex image\" />";
    }

    private String normalizeImageSource(String src) {
        if (src.startsWith("file:")) {
            return "/api/files/" + src.substring("file:".length()) + "/download";
        }
        return src;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
