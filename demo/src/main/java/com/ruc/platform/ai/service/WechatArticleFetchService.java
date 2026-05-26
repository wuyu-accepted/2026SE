package com.ruc.platform.ai.service;

public interface WechatArticleFetchService {
    WechatArticleContent fetch(String url);

    record WechatArticleContent(String title, String content) {
    }
}
