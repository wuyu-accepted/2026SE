package com.ruc.platform.knowledgeness.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "knowledge.intelligence")
public class KnowledgeIntelligenceProperties {

    private Ocr ocr = new Ocr();
    private Search search = new Search();
    private Semantic semantic = new Semantic();
    private Latex latex = new Latex();
    private Indexing indexing = new Indexing();

    @Data
    public static class Ocr {
        private boolean enabled = false;
        private String command = "";
        private String language = "chi_sim+eng";
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Search {
        private String provider = "lucene";
        private String indexPath = "${user.home}/ruc-platform/lucene/knowledge";
        private int maxResults = 50;
    }

    @Data
    public static class Semantic {
        private boolean enabled = true;
        private String synonymPath = "";
    }

    @Data
    public static class Latex {
        private boolean enabled = false;
        private String command = "tectonic";
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Indexing {
        private boolean enabled = true;
        private int maxRetry = 3;
        private int batchSize = 3;
    }
}
