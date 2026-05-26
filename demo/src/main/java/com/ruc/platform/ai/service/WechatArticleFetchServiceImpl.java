package com.ruc.platform.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WechatArticleFetchServiceImpl implements WechatArticleFetchService {
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_TEXT_LENGTH = 4000;
    private static final Pattern TITLE_PATTERN = Pattern.compile("<meta\\s+property=[\"']og:title[\"']\\s+content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_PATTERN = Pattern.compile("<div[^>]+id=[\"']js_content[\"'][^>]*>(.*?)</div>\\s*<script", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public WechatArticleContent fetch(String url) {
        if (url == null || url.isBlank() || !url.startsWith("http")) {
            return null;
        }
        CacheEntry cached = cache.get(url);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.content();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("微信文章抓取失败，status: {}, url: {}", response.statusCode(), url);
                return null;
            }
            WechatArticleContent content = parse(response.body());
            if (content != null && content.content() != null && !content.content().isBlank()) {
                cache.put(url, new CacheEntry(content, Instant.now().plus(CACHE_TTL)));
            }
            return content;
        } catch (Exception e) {
            log.warn("微信文章抓取异常，url: {}, error: {}", url, e.getMessage());
            return null;
        }
    }

    WechatArticleContent parse(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String title = htmlDecode(extractFirst(TITLE_PATTERN, html));
        String rawContent = extractFirst(CONTENT_PATTERN, html);
        String text = cleanupHtml(rawContent.isBlank() ? html : rawContent);
        if (text.length() > MAX_TEXT_LENGTH) {
            text = text.substring(0, MAX_TEXT_LENGTH);
        }
        return text.isBlank() && title.isBlank() ? null : new WechatArticleContent(title, text);
    }

    private String extractFirst(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String cleanupHtml(String html) {
        return htmlDecode(html)
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String htmlDecode(String value) {
        return value == null ? "" : value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private record CacheEntry(WechatArticleContent content, Instant expiresAt) {
    }
}
